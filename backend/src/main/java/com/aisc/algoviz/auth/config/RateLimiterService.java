package com.aisc.algoviz.auth.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý giới hạn tần suất truy cập (Rate Limiting) theo thuật toán Token Bucket.
 * Hỗ trợ các ngưỡng khác nhau cho các endpoint nhạy cảm (auth, submissions, general)
 * và tự động dọn dẹp bộ nhớ định kỳ.
 */
@Slf4j
@Service
public class RateLimiterService {

    @Value("${algoviz.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${algoviz.security.rate-limit.auth.requests-per-minute:20}")
    private int authLimitPerMinute;

    @Value("${algoviz.security.rate-limit.submissions.requests-per-minute:20}")
    private int submissionsLimitPerMinute;

    @Value("${algoviz.security.rate-limit.default.requests-per-minute:120}")
    private int defaultLimitPerMinute;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Getter
    public static class TokenBucket {
        private final long capacity;
        private final double refillRatePerSecond;
        private double availableTokens;
        private long lastRefillTimestamp;

        public TokenBucket(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.availableTokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getRemainingTokens() {
            refill();
            return (long) Math.max(0, availableTokens);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double secondsElapsed = (now - lastRefillTimestamp) / 1000.0;
            if (secondsElapsed > 0) {
                availableTokens = Math.min(capacity, availableTokens + (secondsElapsed * refillRatePerSecond));
                lastRefillTimestamp = now;
            }
        }

        public synchronized boolean isStale(long maxIdleMillis) {
            return (System.currentTimeMillis() - lastRefillTimestamp) > maxIdleMillis;
        }
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final long limit;
        private final long remaining;

        public RateLimitResult(boolean allowed, long limit, long remaining) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getLimit() {
            return limit;
        }

        public long getRemaining() {
            return remaining;
        }
    }

    public RateLimitResult checkLimit(String clientIp, String requestUri) {
        if (!enabled) {
            return new RateLimitResult(true, 1000, 1000);
        }

        int limit = resolveLimit(requestUri);
        double refillRate = limit / 60.0;
        String bucketKey = resolveBucketCategory(requestUri) + ":" + clientIp;

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(limit, refillRate));
        boolean allowed = bucket.tryConsume();
        long remaining = bucket.getRemainingTokens();

        return new RateLimitResult(allowed, limit, remaining);
    }

    private int resolveLimit(String uri) {
        if (uri.startsWith("/api/auth")) {
            return authLimitPerMinute;
        }
        if (uri.startsWith("/api/submissions")) {
            return submissionsLimitPerMinute;
        }
        return defaultLimitPerMinute;
    }

    private String resolveBucketCategory(String uri) {
        if (uri.startsWith("/api/auth")) {
            return "auth";
        }
        if (uri.startsWith("/api/submissions")) {
            return "submissions";
        }
        return "default";
    }

    /**
     * Dọn dẹp các bucket không hoạt động trong hơn 10 phút để tránh rò rỉ bộ nhớ (memory leak).
     */
    @Scheduled(fixedRate = 600_000) // 10 minutes
    public void cleanupStaleBuckets() {
        long tenMinutesMillis = 600_000L;
        buckets.entrySet().removeIf(entry -> entry.getValue().isStale(tenMinutesMillis));
    }

    public void reset() {
        buckets.clear();
    }
}
