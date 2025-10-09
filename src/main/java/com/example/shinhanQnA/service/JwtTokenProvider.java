package com.example.shinhanQnA.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final String secretKey;
    private final Key key;

    private final long accessTokenValidTime = 1000L * 60 * 30;  // 30분
    private final long refreshTokenValidTime = 1000L * 60 * 60 * 24 * 7;  // 7일

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 기존 메서드 - 일반 사용자용 (하위 호환성 유지)
    public String createAccessToken(String userId) {
        return createAccessTokenWithRole(userId, "USER");
    }

    public String createRefreshToken(String userId) {
        return createRefreshTokenWithRole(userId, "USER");
    }

    // 역할(role)을 포함한 액세스 토큰 생성
    public String createAccessTokenWithRole(String userId, String role) {
        String token = Jwts.builder()
                .setSubject(userId)
                .claim("role", role)  // 역할 정보 추가
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        logger.info("생성된 AccessToken: {} (userId: {}, role: {})", token, userId, role);
        return token;
    }

    // 역할(role)을 포함한 리프레시 토큰 생성
    public String createRefreshTokenWithRole(String userId, String role) {
        String token = Jwts.builder()
                .setSubject(userId)
                .claim("role", role)  // 역할 정보 추가
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        logger.info("생성된 RefreshToken: {} (userId: {}, role: {})", token, userId, role);
        return token;
    }

    // 관리자 전용 토큰 생성 메서드
    public String createAdminAccessToken(String adminId) {
        return createAccessTokenWithRole(adminId, "ADMIN");
    }

    public String createAdminRefreshToken(String adminId) {
        return createRefreshTokenWithRole(adminId, "ADMIN");
    }

    public int getAccessTokenValidTimeSeconds() {
        return (int)(accessTokenValidTime / 1000L);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();  // 토큰 subject가 이메일이라고 가정
    }

    // 토큰에서 역할(role) 추출
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    // 토큰이 관리자 토큰인지 판별
    public boolean isAdminToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            String role = getRoleFromToken(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            logger.warn("토큰 역할 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    // 토큰이 일반 사용자 토큰인지 판별
    public boolean isUserToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            String role = getRoleFromToken(token);
            return "USER".equals(role) || role == null; // role이 없는 기존 토큰도 USER로 처리
        } catch (Exception e) {
            logger.warn("토큰 역할 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}
