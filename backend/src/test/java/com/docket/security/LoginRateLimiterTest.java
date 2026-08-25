package com.docket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter();
    }

    @Test
    @DisplayName("User is not blocked initially")
    void testNotBlockedInitially() {
        assertFalse(limiter.isBlocked("user@example.com"));
    }

    @Test
    @DisplayName("Account is locked out after 5 consecutive failed attempts")
    void testLocksAfterMaxAttempts() {
        String email = "victim@example.com";

        for (int i = 1; i <= 4; i++) {
            limiter.recordFailure(email);
            assertFalse(limiter.isBlocked(email), "Should not be blocked after " + i + " attempts");
        }

        limiter.recordFailure(email);
        assertTrue(limiter.isBlocked(email), "Should be blocked after 5 attempts");
    }

    @Test
    @DisplayName("Successful login resets failure counter")
    void testSuccessResetsFailureCounter() {
        String email = "user@example.com";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(email);
        }

        limiter.recordSuccess(email);
        assertFalse(limiter.isBlocked(email));

        // Another 4 failures should still not lock
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(email);
        }
        assertFalse(limiter.isBlocked(email));
    }

    @Test
    @DisplayName("Different email accounts are tracked independently")
    void testAccountsAreIsolated() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(email1);
        }

        assertTrue(limiter.isBlocked(email1));
        assertFalse(limiter.isBlocked(email2));
    }
}
