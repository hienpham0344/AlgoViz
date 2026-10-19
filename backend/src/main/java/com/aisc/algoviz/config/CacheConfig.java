package com.aisc.algoviz.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình hệ thống Cache sử dụng Caffeine in-memory cache engine.
 * Cung cấp hiệu năng đọc cực cao (high read-throughput) với cơ chế tự động hết hạn (TTL)
 * và thu hồi bộ nhớ theo kích thước tối đa (LRU/W-TinyLFU eviction).
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROBLEMS_LIST_CACHE = "problems_list";
    public static final String LEGACY_PROBLEM_DETAIL_CACHE = "legacy_problem_detail";
    public static final String PROBLEM_DETAIL_CACHE = "problem_detail";
    public static final String PROBLEM_DETAIL_SLUG_CACHE = "problem_detail_slug";

    @Value("${algoviz.cache.ttl-minutes:10}")
    private long defaultTtlMinutes;

    @Value("${algoviz.cache.max-size:1000}")
    private long defaultMaxSize;

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing Caffeine CacheManager: TTL = {} min, MaxSize = {}", defaultTtlMinutes, defaultMaxSize);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PROBLEMS_LIST_CACHE,
                LEGACY_PROBLEM_DETAIL_CACHE,
                PROBLEM_DETAIL_CACHE,
                PROBLEM_DETAIL_SLUG_CACHE
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(defaultTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(defaultMaxSize)
                .recordStats());

        return cacheManager;
    }
}
