package com.example.shinhanQnA.service;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Repository
public class RefreshTokenRepository {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenRepository.class);
    private final Map<String, String> refreshTokenStorage = new ConcurrentHashMap<>();

    public void save(String userId, String refreshToken) {
        refreshTokenStorage.put(userId, refreshToken);
        logger.info("리프레시 토큰 저장 (userId: {}, refreshToken: {})", userId, refreshToken);
    }

    public Optional<String> findByUserId(String userId) {
        Optional<String> token = Optional.ofNullable(refreshTokenStorage.get(userId));
        logger.info("리프레시 토큰 조회 (userId: {}, token 존재: {})", userId, token.isPresent());
        return token;
    }

    public void delete(String userId) {
        refreshTokenStorage.remove(userId);
        logger.info("리프레시 토큰 삭제 (userId: {})", userId);
    }
}

