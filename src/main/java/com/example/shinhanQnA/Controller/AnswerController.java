package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.entity.Answer;
import com.example.shinhanQnA.service.AdminService;
import com.example.shinhanQnA.service.AnswerService;
import com.example.shinhanQnA.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminService adminService;

    private String validateAndExtractEmail(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 올바르지 않습니다.");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        return jwtTokenProvider.getEmailFromToken(token);
    }

    private boolean isAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        return adminService.isValidAdminToken(token);
    }

    // 답변 작성 (관리자만 가능)
    @PostMapping
    public ResponseEntity<?> createAnswer(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Answer answer) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        Answer created = answerService.createAnswer(answer);
        return ResponseEntity.ok(created);
    }

    // 답변 수정 (관리자만 가능)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnswer(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestBody Answer answer) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            Answer updated = answerService.updateAnswer(id, answer);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // 답변 삭제 (관리자만 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnswer(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            answerService.deleteAnswer(id);
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // 답변 전체 조회 (유저, 관리자 모두 가능)
    @GetMapping
    public ResponseEntity<?> getAllAnswers(@RequestHeader("Authorization") String authorizationHeader) {
        validateAndExtractEmail(authorizationHeader);
        List<Answer> answers = answerService.getAllAnswers();
        return ResponseEntity.ok(answers);
    }

    // 답변 단일 조회 (유저, 관리자 모두 가능)
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnswerById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        validateAndExtractEmail(authorizationHeader);

        try {
            Answer answer = answerService.getAnswerById(id);
            return ResponseEntity.ok(answer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}

