package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.OauthUserInfo;

public interface OauthService {
    OauthUserInfo getUserInfo(String code);
}

