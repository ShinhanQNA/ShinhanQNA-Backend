package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OauthUserInfo {
    private String provider; // "kakao", "google", "apple" 등
    private String oauthId;  // 소셜 플랫폼 사용자 고유 id
    private String email;
    private String nickname;
    // ... other fields as needed
}

