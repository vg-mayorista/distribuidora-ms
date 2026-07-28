package com.distribuidora.config.security;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Service to manage invalidated JWT tokens in-memory.
 */
@Service
public class InMemoryTokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adds a token to the blacklist with its corresponding expiration date.
     * Only tokens with valid, future expiration dates are stored.
     *
     * @param token          the JWT token to blacklist
     * @param expirationDate the expiration date of the token
     */
    public void blacklistToken(String token, Date expirationDate) {
        if (expirationDate != null && expirationDate.after(new Date())) {
            blacklistedTokens.put(token, expirationDate);
        }
    }

    /**
     * Checks if a token is blacklisted. If the token is found but its expiration
     * date has already passed, it is lazily removed from the map.
     *
     * @param token the JWT token to verify
     * @return true if the token is blacklisted and has not expired yet, false otherwise
     */
    public boolean isBlacklisted(String token) {
        Date expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration.before(new Date())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }
}
