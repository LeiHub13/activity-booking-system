package com.example.activitybookingsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final long ACCESS_TOKEN_EXPIRE_SECONDS = 30 * 60L;
    public static final long REFRESH_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60L;
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = ACCESS_TOKEN_EXPIRE_SECONDS * 1000L;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = REFRESH_TOKEN_EXPIRE_SECONDS * 1000L;
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor("activity-booking-system-secret-key-123456".getBytes());

    public static String generateToken(Long userId, String username, String role) {
        return generateAccessToken(userId, username, role);
    }

    public static String generateAccessToken(Long userId, String username, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .claim("tokenType", TOKEN_TYPE_ACCESS)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("tokenType", TOKEN_TYPE_REFRESH)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
