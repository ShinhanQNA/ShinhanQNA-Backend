package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.OauthUserInfo;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service("kakaoOauthService")
@RequiredArgsConstructor
public class KakaoOauthService implements OauthService {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Override
    public OauthUserInfo getUserInfo(String codeOrToken) {
        log.info("getUserInfo 호출됨, 파라미터: {}", codeOrToken);
        try {
            KakaoUserResponse kakaoUser;

            if (isAccessTokenFormat(codeOrToken)) {
                log.info("파라미터가 액세스 토큰으로 인식됨");
                kakaoUser = requestKakaoUserInfo(codeOrToken);
            } else {
                log.info("파라미터가 인가 코드(authorization code)로 인식됨");
                KakaoTokenResponse kakaoToken = requestKakaoToken(codeOrToken);
                log.info("카카오 토큰 정상 발급됨: {}", kakaoToken);
                kakaoUser = requestKakaoUserInfo(kakaoToken.getAccessToken());
            }

            log.info("카카오 사용자 정보 조회 성공: {}", kakaoUser);

            String email = kakaoUser.getKakaoAccount() != null ? kakaoUser.getKakaoAccount().getEmail() : null;
            if (email == null) {
                log.error("이메일 정보가 없음");
                throw new RuntimeException("이메일 정보가 없습니다");
            }

            String nickname = (kakaoUser.getKakaoAccount() != null && kakaoUser.getKakaoAccount().getProfile() != null)
                    ? kakaoUser.getKakaoAccount().getProfile().getNickname()
                    : "Unknown";

            String serverRefreshToken = jwtTokenProvider.createRefreshToken(email);
            log.info("서버 리프레시 토큰 생성 완료");

            userRepository.findByEmail(email)
                    .map(user -> {
                        log.info("기존 사용자 있음, 토큰 및 이름 업데이트: {}", email);
                        user.setToken(serverRefreshToken);
                        user.setName(nickname);
                        return userRepository.save(user);
                    })
                    .orElseGet(() -> {
                        log.info("신규 사용자 생성: {}", email);
                        User newUser = User.builder()
                                .email(email)
                                .name(nickname)
                                .token(serverRefreshToken)
                                .build();
                        return userRepository.save(newUser);
                    });

            return new OauthUserInfo(
                    "kakao",
                    String.valueOf(kakaoUser.getId()),
                    email,
                    nickname
            );
        } catch (Exception e) {
            log.error("getUserInfo 처리 중 예외 발생: ", e);
            throw e;
        }
    }

    // 액세스 토큰인지 간단히 체크하는 방법 (형태 검증 최소화)
    private boolean isAccessTokenFormat(String tokenOrCode) {
        if (tokenOrCode == null) {
            return false;
        }
        // "Bearer " 접두사가 있으면 액세스 토큰으로 간주
        if (tokenOrCode.startsWith("Bearer ")) {
            return true;
        }

        return true;

    }



    private KakaoTokenResponse requestKakaoToken(String code) {
        log.info("requestKakaoToken 호출됨, 인가 코드: {}", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        //params.add("redirect_uri", "http://localhost:8080/oauth/callback/kakao"); // 반드시 앱 등록 redirect-uri와 일치해야함
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(tokenUri, request, KakaoTokenResponse.class);

        log.info("카카오 토큰 API 응답: 상태코드={}, 본문={}", response.getStatusCode(), response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("카카오 토큰 발급 실패, 상태코드={}, 응답본문={}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("카카오 토큰 발급 실패");
        }
        return response.getBody();
    }

    private KakaoUserResponse requestKakaoUserInfo(String accessToken) {
        log.info("requestKakaoUserInfo 호출됨, 액세스 토큰: {}", accessToken); // 토큰 null 체크용
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // 이 줄이 실행될 때 accessToken이 null이면 안 됨

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, KakaoUserResponse.class);

        log.info("카카오 사용자 정보 API 응답: 상태코드={}, 본문={}", response.getStatusCode(), response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("카카오 사용자 정보 조회 실패, 상태코드={}, 응답본문={}", response.getStatusCode(), response.getBody());
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

        @Override
        public String toString() {
            return "KakaoTokenResponse{" +
                    "access_token='" + access_token + '\'' +
                    ", refresh_token='" + refresh_token + '\'' +
                    ", expires_in=" + expires_in +
                    ", refresh_token_expires_in=" + refresh_token_expires_in +
                    '}';
        }
    }

    // 내부 DTO 클래스: 사용자 정보 응답
    @Data
    public static class KakaoUserResponse {
        private Long id;
        private KakaoAccount kakao_account;

        public Long getId() { return id; }
        public KakaoAccount getKakaoAccount() { return kakao_account; }

        @Override
        public String toString() {
            return "KakaoUserResponse{" +
                    "id=" + id +
                    ", kakao_account=" + kakao_account +
                    '}';
        }

        @Data
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            public String getEmail() { return email; }
            public Profile getProfile() { return profile; }

            @Override
            public String toString() {
                return "KakaoAccount{" +
                        "email='" + email + '\'' +
                        ", profile=" + profile +
                        '}';
            }

            @Data
            public static class Profile {
                private String nickname;
                public String getNickname() { return nickname; }

                @Override
                public String toString() {
                    return "Profile{" +
                            "nickname='" + nickname + '\'' +
                            '}';
                }
            }
        }
    }
}
