package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.entity.Notice;
import com.example.shinhanQnA.service.AdminService;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
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

    // 공지사항 작성 (관리자만 가능)
    @PostMapping
    public ResponseEntity<?> createNotice(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Notice notice) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        Notice created = noticeService.createNotice(notice);
        return ResponseEntity.ok(created);
    }

    // 공지사항 수정 (관리자만 가능)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotice(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestBody Notice notice) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            Notice updated = noticeService.updateNotice(id, notice);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // 공지사항 삭제 (관리자만 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotice(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            noticeService.deleteNotice(id);
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // 공지사항 전체 조회 (유저, 관리자 모두 가능)
    @GetMapping
    public ResponseEntity<?> getAllNotices(@RequestHeader("Authorization") String authorizationHeader) {
        validateAndExtractEmail(authorizationHeader);  // 토큰만 있으면 OK
        List<Notice> notices = noticeService.getAllNotices();
        return ResponseEntity.ok(notices);
    }

    // 공지사항 단일 조회 (유저, 관리자 모두 가능)
    @GetMapping("/{id}")
    public ResponseEntity<?> getNoticeById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        validateAndExtractEmail(authorizationHeader);  // 토큰만 있으면 OK

        try {
            Notice notice = noticeService.getNoticeById(id);
            return ResponseEntity.ok(notice);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
