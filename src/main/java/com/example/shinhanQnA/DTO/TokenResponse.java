package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TokenResponse {
    private String accessToken;      // 서버(JWT)
    private String refreshToken;     // 서버(JWT)
    private String oauthAccessToken; // 필요하면 소셜 제공자 액세스 토큰
    private String oauthRefreshToken;// 필요하면 소셜 제공자 리프레시 토큰
}

