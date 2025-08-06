package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.OauthUserInfo;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.UserRepository;
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
    private final UserRepository userRepository;

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

        // 3. 이메일 추출 및 유효성 검사
        String email = googleUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("이메일 정보가 없습니다");
        }

        // 4. JWT 생성 (subject를 이메일로)
        String serverAccessToken = jwtTokenProvider.createAccessToken(email);
        String serverRefreshToken = jwtTokenProvider.createRefreshToken(email);

        // 5. 리프레시 토큰을 DB에 저장 또는 사용자 신규 생성
        userRepository.findByEmail(email)
                .map(user -> {
                    // 기존 사용자 토큰 업데이트 및 이름 업데이트
                    user.setToken(serverRefreshToken);
                    user.setName(googleUser.getName());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // 신규 사용자 생성 및 리프레시 토큰 저장
                    User newUser = User.builder()
                            .email(email)
                            .name(googleUser.getName())
                            .token(serverRefreshToken)
                            .build();
                    return userRepository.save(newUser);
                });

        // 6. OauthUserInfo 반환: oauthId는 googleUser.getSub() 등 구글 고유 ID 사용
        return new OauthUserInfo(
                "google",
                googleUser.getSub(),  // 고유 ID 유지
                email,
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
