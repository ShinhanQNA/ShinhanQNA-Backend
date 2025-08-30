package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appeals")
public class AppealController {

    private final AppealService appealService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final BoardService boardService;
    private final AdminService adminService; // 관리자 권한 검증용

    private String validateAndExtractEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 잘못되었습니다.");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("잘못된 토큰입니다.");
        }
        return jwtTokenProvider.getEmailFromToken(token);
    }

    // 사용자 이의제기 신청 (차단 상태인 사용자만)
    @PostMapping
    public ResponseEntity<?> submitAppeal(@RequestHeader("Authorization") String authHeader) {
        try {
            String email = validateAndExtractEmail(authHeader);
            return ResponseEntity.ok(appealService.submitAppeal(email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}




