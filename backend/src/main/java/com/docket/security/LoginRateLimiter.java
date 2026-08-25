package com.docket.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * In-memory brute-force protection for login attempts.
 * Tracks failed authentication attempts per email key and enforces a temporary lockout.
 */
@Component
public class LoginRateLimiter {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_SECONDS = 900; // 15 minutes

    private final ConcurrentMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * Checks if the given key (e.g. email) is currently locked out from attempting login.
     *
     * @param key the identifier (typically lowercase email)
     * @return true if currently locked out, false otherwise
     */
    public boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        String normalizedKey = key.trim().toLowerCase();
        AttemptRecord record = attempts.get(normalizedKey);
        if (record == null) {
            return false;
        }

        if (record.isExpired(LOCKOUT_DURATION_SECONDS)) {
            attempts.remove(normalizedKey);
            return false;
        }

        return record.attemptCount >= MAX_ATTEMPTS;
    }

    /**
     * Records a failed login attempt for the key.
     *
     * @param key the identifier
     * @return the total number of recorded failures within the current window
     */
    public int recordFailure(String key) {
        if (key == null) {
            return 0;
        }
        String normalizedKey = key.trim().toLowerCase();
        AttemptRecord updated = attempts.compute(normalizedKey, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.isExpired(LOCKOUT_DURATION_SECONDS)) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(existing.attemptCount + 1, existing.firstAttempt);
        });

        return updated.attemptCount;
    }

    /**
     * Resets the failure counter on a successful login.
     *
     * @param key the identifier
     */
    public void recordSuccess(String key) {
        if (key != null) {
            attempts.remove(key.trim().toLowerCase());
        }
    }

    /**
     * Clears all attempt records (useful for test resets).
     */
    public void resetAll() {
        attempts.clear();
    }

    private static class AttemptRecord {
        final int attemptCount;
        final Instant firstAttempt;

        AttemptRecord(int attemptCount, Instant firstAttempt) {
            this.attemptCount = attemptCount;
            this.firstAttempt = firstAttempt;
        }

        boolean isExpired(long durationSeconds) {
            return Instant.now().isAfter(firstAttempt.plusSeconds(durationSeconds));
        }
    }
}
