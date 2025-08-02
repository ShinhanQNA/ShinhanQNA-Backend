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

@Service("kakaoOauthService")
@RequiredArgsConstructor
public class KakaoOauthService implements OauthService {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    @Override
    public OauthUserInfo getUserInfo(String code) {
        // 1. 인가코드로 카카오 토큰 요청
        KakaoTokenResponse kakaoToken = requestKakaoToken(code);

        // 2. 토큰으로 카카오 사용자 정보 조회
        KakaoUserResponse kakaoUser = requestKakaoUserInfo(kakaoToken.getAccessToken());

        // 3. 서버 JWT 발급 및 리프레시 토큰 저장 (필요 시 다른 계층에서 처리 가능)
        String userIdStr = String.valueOf(kakaoUser.getId());

        String serverAccessToken = jwtTokenProvider.createAccessToken(userIdStr);
        String serverRefreshToken = jwtTokenProvider.createRefreshToken(userIdStr);

        refreshTokenRepository.save(userIdStr, serverRefreshToken);

        // 4. OauthUserInfo 변환하여 반환
        return new OauthUserInfo(
                "kakao",
                userIdStr,
                kakaoUser.getKakaoAccount() != null ? kakaoUser.getKakaoAccount().getEmail() : null,
                (kakaoUser.getKakaoAccount() != null && kakaoUser.getKakaoAccount().getProfile() != null)
                        ? kakaoUser.getKakaoAccount().getProfile().getNickname() : null
        );
    }

    private KakaoTokenResponse requestKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", "http://localhost:8080/oauth/callback/kakao"); // 반드시 앱 등록 redirect-uri와 일치해야함
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(tokenUri, request, KakaoTokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("카카오 토큰 발급 실패");
        }

        return response.getBody();
    }

    private KakaoUserResponse requestKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, KakaoUserResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }

        return response.getBody();
    }

    // 내부 DTO 클래스: 토큰 응답
    @Data
    public static class KakaoTokenResponse {
        private String access_token;
        private String refresh_token;
        private long expires_in;
        private long refresh_token_expires_in;

        public String getAccessToken() { return access_token; }
        public String getRefreshToken() { return refresh_token; }
    }

    // 내부 DTO 클래스: 사용자 정보 응답
    @Data
    public static class KakaoUserResponse {
        private Long id;
        private KakaoAccount kakao_account;

        public Long getId() { return id; }
        public KakaoAccount getKakaoAccount() { return kakao_account; }

        @Data
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            public String getEmail() { return email; }
            public Profile getProfile() { return profile; }

            @Data
            public static class Profile {
                private String nickname;
                public String getNickname() { return nickname; }
            }
        }
    }
}
