package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String access_token;   // 서버 JWT Access Token
    private String refresh_token;  // 서버 JWT Refresh Token
    private int expires_in;        // Access Token 만료 시간(초)
}
