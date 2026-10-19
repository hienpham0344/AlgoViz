package com.aisc.algoviz.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance In-Memory Access Token Blacklist for immediate revocation upon Logout.
 * Auto-evicts expired tokens to keep memory footprint minimal.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiry) {
        if (token != null && expiry != null && expiry.isAfter(Instant.now())) {
            blacklistedTokens.put(token, expiry);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        Instant expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }
}
