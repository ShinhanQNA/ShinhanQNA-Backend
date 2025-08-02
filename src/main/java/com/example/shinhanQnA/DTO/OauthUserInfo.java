package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OauthUserInfo {
    private String provider;    // "kakao", "google" 등 플랫폼
    private String oauthId;     // 소셜 고유 ID (String)
    private String email;       // 이메일 (없는 경우 null 가능)
    private String nickname;    // 닉네임 (없는 경우 null 가능)
}
