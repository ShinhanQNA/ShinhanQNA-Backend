package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.OauthUserInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service("googleOauthService")
@RequiredArgsConstructor
public class GoogleOauthService implements OauthService {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Override
    public OauthUserInfo getUserInfo(String code) {
        // 1. 인가코드로 구글 토큰 요청
        GoogleTokenResponse googleToken = requestGoogleToken(code);

        // 2. 토큰으로 구글 사용자 정보 조회
        GoogleUserResponse googleUser = requestGoogleUserInfo(googleToken.getAccessToken());

        // 3. 서버 JWT 발급 및 리프레시 토큰 저장 (필요 시 다른 계층에서 처리 가능)
        String userIdStr = googleUser.getSub(); // Google user unique ID

        String serverAccessToken = jwtTokenProvider.createAccessToken(userIdStr);
        String serverRefreshToken = jwtTokenProvider.createRefreshToken(userIdStr);

        refreshTokenRepository.save(userIdStr, serverRefreshToken);

        // 4. OauthUserInfo 변환해 반환
        return new OauthUserInfo(
                "google",
                userIdStr,
                googleUser.getEmail(),
                googleUser.getName()
        );
    }

    private GoogleTokenResponse requestGoogleToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(tokenUri, request, GoogleTokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("구글 토큰 발급 실패");
        }
        return response.getBody();
    }

    private GoogleUserResponse requestGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, GoogleUserResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("구글 사용자 정보 조회 실패");
        }
        return response.getBody();
    }

    // 구글 토큰 응답 DTO
    @Data
    public static class GoogleTokenResponse {
        private String access_token;
        private String refresh_token;
        private String scope;
        private String token_type;
        private Integer expires_in;

        public String getAccessToken() {
            return access_token;
        }

        public String getRefreshToken() {
            return refresh_token;
        }
    }

    // 구글 사용자 정보 DTO
    @Data
    public static class GoogleUserResponse {
        private String sub; // 유니크 구글 사용자 ID
        private String name;
        private String given_name;
        private String family_name;
        private String picture;
        private String email;
        private boolean email_verified;
        private String locale;
    }
}
