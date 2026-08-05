package com.syncdocs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbWluaW11bS0zMi1ieXRlcw==";
    private static final long EXPIRATION = 3600000;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_ShouldCreateValidJwt() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalseForNullToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_ShouldReturnFalseForEmptyToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void validateToken_ShouldReturnFalseForMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.jwt"));
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_ShouldReturnFalseForTamperedToken() {
        String token = jwtTokenProvider.generateToken("testuser");
        // Tamper with the payload section
        int firstDot = token.indexOf('.');
        int secondDot = token.indexOf('.', firstDot + 1);
        String tampered = token.substring(0, firstDot + 1)
                + "X" + token.substring(firstDot + 2, secondDot)
                + token.substring(secondDot);
        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectSubject() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void getUsernameFromToken_ShouldThrowForInvalidToken() {
        assertThrows(Exception.class, () -> jwtTokenProvider.getUsernameFromToken("invalid"));
    }

    @Test
    void getUsernameFromToken_ShouldThrowForExpiredToken() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
        assertThrows(Exception.class, () -> jwtTokenProvider.getUsernameFromToken(expiredToken));
    }
}
