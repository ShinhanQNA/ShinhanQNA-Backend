package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.OauthUserInfo;
import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.service.OauthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.RefreshTokenRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequiredArgsConstructor
public class OauthController {
    private static final Logger logger = LoggerFactory.getLogger(OauthController.class);

    private final Map<String, OauthService> oauthServiceMap;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @GetMapping("/oauth/callback/{provider}")
    public ResponseEntity<TokenResponse> socialCallback(
            @PathVariable String provider, @RequestParam String code) {

        logger.info("OAuth 콜백 요청 (provider: {}, code: {})", provider, code);

        OauthService oauthService = getOauthService(provider);
        OauthUserInfo userInfo = oauthService.getUserInfo(code);

        logger.info("OauthUserInfo 획득 (userId: {}, email: {})", userInfo.getOauthId(), userInfo.getEmail());

        String accessToken = jwtTokenProvider.createAccessToken(userInfo.getOauthId());
        String refreshToken = jwtTokenProvider.createRefreshToken(userInfo.getOauthId());

        refreshTokenRepository.save(userInfo.getOauthId(), refreshToken);

        logger.info("로그인 토큰 생성 완료 (accessToken: {}, refreshToken: {})", accessToken, refreshToken);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, null, null));
    }

    private OauthService getOauthService(String provider) {
        String beanName = provider.toLowerCase() + "OauthService";
        OauthService oauthService = oauthServiceMap.get(beanName);
        if (oauthService == null) throw new IllegalArgumentException("지원하지 않는 소셜 로그인: " + provider);
        return oauthService;
    }
}

