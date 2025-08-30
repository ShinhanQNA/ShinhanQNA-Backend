package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.entity.Appeal;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/appeals")
public class AdminAppealController {

    private final AppealService appealService;
    private final UserService userService;
    private final BoardService boardService;
    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    private boolean isAuthorizedAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7);
        return adminService.isValidAdminToken(token);
    }

    // 관리자: 전체 이의제기 목록 조회
    @GetMapping
    public ResponseEntity<?> getAllAppeals(@RequestHeader("Authorization") String authorizationHeader) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        // 최신순 정렬된 이의제기 목록 가져오기 (서비스에서 정렬 보장)
        List<Map<String, Object>> response = appealService.getAllPendingAppealsSortedByDate().stream()
                .map(appeal -> {
                    User user = userService.getUserInfo(appeal.getEmail());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", appeal.getId());
                    map.put("name", user.getName());
                    map.put("students", user.getStudents());
                    map.put("year", user.getYear());
                    map.put("department", user.getDepartment());
                    map.put("createdAt", appeal.getCreatedAt());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    // 관리자: 특정 사용자 이의제기 상세 + 게시글 목록 조회
    @GetMapping("/{email}")
    public ResponseEntity<?> getAppealDetail(@RequestHeader("Authorization") String authorizationHeader,
                                             @PathVariable String email) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        try {
            User user = userService.getUserInfo(email);
            List<?> boards = boardService.findAllBoardsWithUserWarning().stream()
                    .filter(b -> b.getWriterEmail().equals(email))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "id", user.getEmail(),
                    "name", user.getName(),
                    "students", user.getStudents(),
                    "year", user.getYear(),
                    "department", user.getDepartment(),
                    "boards", boards
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 관리자: 특정 게시글 상세 조회 (게시글 더 상세 정보)
    @GetMapping("/{email}/boards/{postId}")
    public ResponseEntity<?> getBoardDetailForAppeal(@RequestHeader("Authorization") String authorizationHeader,
                                                     @PathVariable String email,
                                                     @PathVariable Integer postId) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        return boardService.findBoardResponseById(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 이의제기 상태 변경 API (승인/거절)
    @PutMapping("/{appealId}/status")
    public ResponseEntity<?> updateAppealStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long appealId,
            @RequestBody Map<String, String> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        String newStatus = payload.get("status");
        if (newStatus == null || (!newStatus.equals("승인") && !newStatus.equals("거절"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "상태는 '승인' 또는 '거절'만 가능합니다."));
        }

        try {
            Appeal updatedAppeal = appealService.updateAppealStatusAndSyncUser(appealId, newStatus);
            return ResponseEntity.ok(updatedAppeal);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
