package com.aisc.algoviz.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new TokenBlacklistService();
    }

    @Test
    @DisplayName("Should successfully blacklist a valid token")
    void testBlacklistValidToken() {
        String token = "sample-jwt-token-123";
        Instant expiry = Instant.now().plus(15, ChronoUnit.MINUTES);

        blacklistService.blacklistToken(token, expiry);

        assertTrue(blacklistService.isBlacklisted(token));
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token or null")
    void testNonBlacklistedToken() {
        assertFalse(blacklistService.isBlacklisted("unknown-token"));
        assertFalse(blacklistService.isBlacklisted(null));
    }

    @Test
    @DisplayName("Should not blacklist token if expiry is already in the past")
    void testExpiredTokenNotAdded() {
        String token = "already-expired-token";
        Instant pastExpiry = Instant.now().minus(5, ChronoUnit.MINUTES);

        blacklistService.blacklistToken(token, pastExpiry);

        assertFalse(blacklistService.isBlacklisted(token));
    }

    @Test
    @DisplayName("Should auto-evict token once expired when queried")
    void testAutoEvictExpiredToken() {
        String token = "short-lived-token";
        // 1 millisecond into future, then wait brief moment
        Instant nearFuture = Instant.now().plusMillis(50);
        blacklistService.blacklistToken(token, nearFuture);

        try {
            Thread.sleep(70);
        } catch (InterruptedException ignored) {}

        assertFalse(blacklistService.isBlacklisted(token));
    }
}
