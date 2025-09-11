package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.DTO.OauthUserInfo;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.OauthService;
import com.example.shinhanQnA.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OauthController {

    private static final Logger logger = LoggerFactory.getLogger(OauthController.class);

    private final Map<String, OauthService> oauthServiceMap;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/oauth/callback/{provider}")
    public ResponseEntity<?> socialCallback(
            @PathVariable String provider,
            @RequestHeader("Authorization") String code
    ) {
        logger.info("소셜 로그인 진입 – provider: {}", provider);

        try {
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8.name());
            logger.info("디코딩 전 code: {}", code);
            logger.info("디코딩 후 code: {}", decodedCode);
            logger.info("현재 등록된 OauthService 빈 목록: {}", oauthServiceMap.keySet());

            OauthService oauthService = getOauthService(provider);
            logger.info("getOauthService 호출 결과 – provider: {}, 서비스 클래스: {}", provider, oauthService.getClass().getSimpleName());

            OauthUserInfo userInfo = oauthService.getUserInfo(decodedCode);
            logger.info("userInfo 반환: {}", userInfo);

            if (userInfo == null || userInfo.getOauthId() == null) {
                logger.info("사용자 정보 없음 - 소셜 고유 ID 누락");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "이름 없음"));
            }

            if (userInfo.getEmail() == null) {
                throw new RuntimeException("이메일 정보가 없습니다");
            }

            // 사용자 존재 여부 확인 및 신규 생성
            User user;
            try {
                user = userService.getUserInfo(userInfo.getEmail());
                if (user.getStatus() == null) {
                    user.setStatus("가입 대기 중");
                    userService.saveUser(user);
                }
            } catch (RuntimeException e) {
                user = new User();
                user.setEmail(userInfo.getEmail());
                user.setName(userInfo.getNickname());
                user.setStatus("가입 대기 중");  // 기본 상태 지정
                logger.info("[OauthController] 신규 사용자 생성 직후 상태: {}", user.getStatus());
                userService.saveUser(user);
            }

            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

            int expiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();

            logger.info("로그인 토큰 생성 완료 (accessToken: {}, refreshToken: {})", accessToken, refreshToken);

            return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, expiresIn));

        } catch (IllegalArgumentException e) {
            logger.error("지원하지 않는 OAuth 제공자: {} 또는 bean 이름 불일치", provider, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "이름 없음"));
        } catch (Exception e) {
            logger.error("서버 토큰 처리 중 에러", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 에러"));
        }
    }

    private OauthService getOauthService(String provider) {
        String trimmedProvider = provider.trim();  // 앞뒤 공백 제거
        String beanName = trimmedProvider.toLowerCase() + "OauthService";

        logger.info("getOauthService – 요청된 beanName: '{}'", beanName);

        oauthServiceMap.keySet().forEach(key -> {
            logger.info("key='{}', equals(beanName)={}", key, key.equals(beanName));
        });

        OauthService oauthService = oauthServiceMap.get(beanName);
        if (oauthService == null) {
            logger.error("Bean을 찾지 못함: '{}'", beanName);
            throw new IllegalArgumentException();
        }
        return oauthService;
    }
}
