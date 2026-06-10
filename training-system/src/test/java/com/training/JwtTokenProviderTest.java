package com.training;

import com.training.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private static final String SECRET =
            "dGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIGp3dCB0b2tlbiBnZW5lcmF0aW9uIGluIHRyYWluaW5nIHN5c3RlbQ==";
    private static final long ACCESS_EXPIRATION = 7200000L;  // 2 hours
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 days

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("generateAccessToken: should return a non-null, non-empty token")
    void generateAccessToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "admin", "ADMIN");

        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT tokens have 3 parts separated by dots
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("validateToken: should return true for a freshly generated valid token")
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "admin", "ADMIN");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken: should return false for an expired token")
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Create a provider with 1ms expiration so the token expires almost immediately
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L, 1L);
        String token = shortLived.generateAccessToken(1L, "admin", "ADMIN");

        // Wait briefly for the token to expire
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertFalse(shortLived.validateToken(token));
    }

    @Test
    @DisplayName("validateToken: should return false for a malformed token string")
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.jwt.token"));
    }

    @Test
    @DisplayName("validateToken: should return false for a token signed with a different key")
    void validateToken_shouldReturnFalseForWrongKey() {
        // A different secret (same length, different content)
        String differentSecret =
                "YW5vdGhlciB2ZXJ5IHNlY3VyZSBzZWNyZXQga2V5IGZvciBqd3QgdG9rZW4gZ2VuZXJhdGlvbiE=";
        JwtTokenProvider otherProvider = new JwtTokenProvider(differentSecret, ACCESS_EXPIRATION, REFRESH_EXPIRATION);

        String token = otherProvider.generateAccessToken(1L, "admin", "ADMIN");

        // Our main provider should reject a token signed by a different key
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("getUserIdFromToken: should return the correct userId embedded in the token")
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtTokenProvider.generateAccessToken(42L, "student1", "STUDENT");

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getUserIdFromToken: should work with different userId values")
    void getUserIdFromToken_shouldWorkWithVariousIds() {
        Long[] testIds = {1L, 100L, 999999L, Long.MAX_VALUE};

        for (Long expectedId : testIds) {
            String token = jwtTokenProvider.generateAccessToken(expectedId, "user" + expectedId, "STUDENT");
            assertEquals(expectedId, jwtTokenProvider.getUserIdFromToken(token),
                    "Failed for userId: " + expectedId);
        }
    }

    @Test
    @DisplayName("getRoleFromToken: should return the correct role embedded in the token")
    void getRoleFromToken_shouldReturnCorrectRole() {
        String token = jwtTokenProvider.generateAccessToken(1L, "instructor1", "INSTRUCTOR");

        String role = jwtTokenProvider.getRoleFromToken(token);

        assertEquals("INSTRUCTOR", role);
    }

    @Test
    @DisplayName("getRoleFromToken: should return ADMIN role correctly")
    void getRoleFromToken_shouldReturnAdminRole() {
        String token = jwtTokenProvider.generateAccessToken(1L, "admin1", "ADMIN");

        assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("getUsernameFromToken: should return the correct username (subject)")
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String token = jwtTokenProvider.generateAccessToken(1L, "auditor1", "AUDITOR");

        assertEquals("auditor1", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("generateRefreshToken: should return a valid token that passes validation")
    void generateRefreshToken_shouldReturnValidToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(42L);

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));

        // Refresh token stores userId as subject, not as a claim
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getExpirationFromToken: should return a future timestamp for a valid token")
    void getExpirationFromToken_shouldReturnFutureTimestamp() {
        String token = jwtTokenProvider.generateAccessToken(1L, "admin", "ADMIN");

        long expiration = jwtTokenProvider.getExpirationFromToken(token);

        assertTrue(expiration > System.currentTimeMillis());
        // Should be approximately 2 hours from now
        long twoHoursMs = 7200000L;
        assertTrue(expiration - System.currentTimeMillis() <= twoHoursMs);
        assertTrue(expiration - System.currentTimeMillis() > twoHoursMs - 5000);
    }
}
