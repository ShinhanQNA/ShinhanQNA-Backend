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
        // 1. 인가코드로 카카오 토큰 발급 요청
        KakaoTokenResponse kakaoToken = requestKakaoToken(code);

        // 2. 카카오 API로 사용자 정보 조회
        KakaoUserResponse kakaoUser = requestKakaoUserInfo(kakaoToken.getAccessToken());

        // 3. 서버 JWT 발급 및 리프레시 토큰 저장
        // (외부 호출이 아닌 상위 서비스에서 처리할 수도 있음)
        String serverAccessToken = jwtTokenProvider.createAccessToken(kakaoUser.getId());
        String serverRefreshToken = jwtTokenProvider.createRefreshToken(kakaoUser.getId());

        refreshTokenRepository.save(kakaoUser.getId(), serverRefreshToken);

        // 4. OauthUserInfo로 변환해 반환
        return new OauthUserInfo(
                "kakao",
                kakaoUser.getId(),
                kakaoUser.getKakaoAccount().getEmail(),
                kakaoUser.getKakaoAccount().getProfile().getNickname()
        );
    }

    private KakaoTokenResponse requestKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", "http://localhost:8080/oauth/callback/kakao"); // 실제 redirect-uri로 변경
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                tokenUri,
                request,
                KakaoTokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("카카오 토큰 발급 실패");
        }
        return response.getBody();
    }

    private KakaoUserResponse requestKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                KakaoUserResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }
        return response.getBody();
    }

    // 내부 DTO 클래스 예시 (카카오 API 응답이 복잡하면 별도 파일로 분리 가능)
    @Data
    public static class KakaoTokenResponse {
        private String access_token;
        private String refresh_token;
        private long expires_in;
        private long refresh_token_expires_in;

        public String getAccessToken() { return access_token; }
        public String getRefreshToken() { return refresh_token; }
    }

    @Data
    public static class KakaoUserResponse {
        private String id;
        private KakaoAccount kakao_account;

        public String getId() { return id; }
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

