package com.enterprise.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * Handles token creation, validation, and extraction of user information.
 * 
 * Requirements addressed:
 * - 4.1: JWT-based authentication with token generation and validation
 * - 4.2: Token signature validation and expiration handling
 * - 4.3: Claims extraction for user roles and permissions
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private static final String AUTHORITIES_KEY = "authorities";
    private static final String USER_ID_KEY = "userId";
    private static final String EMAIL_KEY = "email";
    
    private final SecretKey secretKey;
    private final long jwtExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:mySecretKey}") String jwtSecret,
            @Value("${app.jwt.expiration:86400000}") long jwtExpirationMs,
            @Value("${app.jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs) {
        
        // Generate a secure key from the secret
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        
        logger.info("JWT Token Provider initialized with expiration: {} ms", jwtExpirationMs);
    }

    /**
     * Generates a JWT access token for the authenticated user.
     * 
     * @param authentication the authentication object containing user details
     * @return the generated JWT token
     */
    public String generateToken(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);
        
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim(USER_ID_KEY, userPrincipal.getId())
                .claim(EMAIL_KEY, userPrincipal.getEmail())
                .claim(AUTHORITIES_KEY, authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a JWT refresh token for the authenticated user.
     * 
     * @param authentication the authentication object containing user details
     * @return the generated refresh token
     */
    public String generateRefreshToken(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim(USER_ID_KEY, userPrincipal.getId())
                .claim("tokenType", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT token signature and expiration.
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts the username from the JWT token.
     * 
     * @param token the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }

    /**
     * Extracts the user ID from the JWT token.
     * 
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get(USER_ID_KEY, Long.class);
    }

    /**
     * Extracts the email from the JWT token.
     * 
     * @param token the JWT token
     * @return the email
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get(EMAIL_KEY, String.class);
    }

    /**
     * Extracts the authorities from the JWT token.
     * 
     * @param token the JWT token
     * @return the collection of granted authorities
     */
    public Collection<? extends GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        String authorities = claims.get(AUTHORITIES_KEY, String.class);
        
        if (authorities == null || authorities.isEmpty()) {
            return List.of();
        }
        
        return List.of(authorities.split(","))
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets the expiration date from the JWT token.
     * 
     * @param token the JWT token
     * @return the expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getExpiration();
    }

    /**
     * Checks if the token is expired.
     * 
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Gets the remaining time until token expiration in milliseconds.
     * 
     * @param token the JWT token
     * @return the remaining time in milliseconds, or 0 if expired
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingTime);
        } catch (Exception e) {
            logger.error("Error calculating token remaining time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if the token is a refresh token.
     * 
     * @param token the JWT token
     * @return true if it's a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            logger.error("Error checking if token is refresh token: {}", e.getMessage());
            return false;
        }
    }
}