package com.example.activitybookingsystem.utils;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Date;


public class JwtUtil {
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor("activity-booking-system-secret-key-123456".getBytes());

    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }
}
