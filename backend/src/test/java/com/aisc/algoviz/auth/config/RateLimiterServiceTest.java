package com.aisc.algoviz.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "enabled", true);
        ReflectionTestUtils.setField(rateLimiterService, "authLimitPerMinute", 3);
        ReflectionTestUtils.setField(rateLimiterService, "submissionsLimitPerMinute", 2);
        ReflectionTestUtils.setField(rateLimiterService, "defaultLimitPerMinute", 5);
        rateLimiterService.reset();
    }

    @Test
    @DisplayName("Should allow requests within limit")
    void testAllowRequestsWithinLimit() {
        String ip = "192.168.1.100";
        String uri = "/api/auth/login";

        RateLimiterService.RateLimitResult res1 = rateLimiterService.checkLimit(ip, uri);
        assertTrue(res1.isAllowed());
        assertEquals(3, res1.getLimit());
        assertEquals(2, res1.getRemaining());

        RateLimiterService.RateLimitResult res2 = rateLimiterService.checkLimit(ip, uri);
        assertTrue(res2.isAllowed());
        assertEquals(1, res2.getRemaining());

        RateLimiterService.RateLimitResult res3 = rateLimiterService.checkLimit(ip, uri);
        assertTrue(res3.isAllowed());
        assertEquals(0, res3.getRemaining());
    }

    @Test
    @DisplayName("Should reject requests when limit is exceeded")
    void testRejectWhenLimitExceeded() {
        String ip = "192.168.1.101";
        String uri = "/api/submissions/run"; // limit is 2

        assertTrue(rateLimiterService.checkLimit(ip, uri).isAllowed());
        assertTrue(rateLimiterService.checkLimit(ip, uri).isAllowed());

        // 3rd request must be rejected
        RateLimiterService.RateLimitResult rejected = rateLimiterService.checkLimit(ip, uri);
        assertFalse(rejected.isAllowed());
        assertEquals(0, rejected.getRemaining());
    }

    @Test
    @DisplayName("Different IPs should have isolated rate buckets")
    void testIsolatedBucketsPerIp() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";
        String uri = "/api/submissions/run"; // limit 2

        rateLimiterService.checkLimit(ip1, uri);
        rateLimiterService.checkLimit(ip1, uri);
        assertFalse(rateLimiterService.checkLimit(ip1, uri).isAllowed());

        // ip2 should still be allowed
        assertTrue(rateLimiterService.checkLimit(ip2, uri).isAllowed());
    }

    @Test
    @DisplayName("Should always allow when rate limiting is disabled")
    void testBypassWhenDisabled() {
        ReflectionTestUtils.setField(rateLimiterService, "enabled", false);
        String ip = "192.168.1.200";

        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiterService.checkLimit(ip, "/api/auth/login").isAllowed());
        }
    }
}
