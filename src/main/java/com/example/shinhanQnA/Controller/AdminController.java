package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.entity.Admin;
import com.example.shinhanQnA.entity.BoardReport;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.entity.userwarning;
import com.example.shinhanQnA.service.AdminService;
import com.example.shinhanQnA.service.BoardReportService;
import com.example.shinhanQnA.service.UserService;
import com.example.shinhanQnA.service.UserWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.shinhanQnA.repository.UserRepository;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserWarningService userWarningService;
    private final BoardReportService boardReportService;

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> loginRequest) {
        String id = loginRequest.get("id");
        String password = loginRequest.get("password");

        logger.info("관리자 로그인 API 요청: id={}", id);

        if (id == null || password == null) {
            logger.warn("아이디 또는 비밀번호 미입력: id={}, passwordNull={}", id, password == null);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "아이디와 비밀번호를 입력하세요."));
        }

        try {
            TokenResponse tokenResponse = adminService.loginAdmin(id, password);
            return ResponseEntity.ok(tokenResponse);
        } catch (RuntimeException e) {
            logger.warn("관리자 로그인 실패: id={}, 원인={}", id, e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // 관리자 계정 등록 API
    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody Map<String, String> req) {
        String id = req.get("id");
        String password = req.get("password");
        String name = req.get("name");

        logger.info("관리자 계정 생성 요청: id={}, name={}", id, name);

        if (id == null || password == null || name == null) {
            logger.warn("필수값 누락: id={}, password={}, name={}", id, password, name);
            return ResponseEntity.badRequest().body(Map.of("error", "아이디, 비밀번호, 이름 모두 입력하세요."));
        }

        try {
            Admin admin = adminService.createAdmin(id, password, name);
            return ResponseEntity.ok(Map.of("id", admin.getId(), "name", admin.getName()));
        } catch (RuntimeException e) {
            logger.warn("관리자 계정 생성 실패: id={}, 원인={}", id, e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authorizationHeader) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }
        return ResponseEntity.ok(userRepository.findAll());
    }


    @PostMapping("/users/info")
    public ResponseEntity<?> getUserInfoByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력하세요."));
        }

        return userRepository.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok) // ★ 이 부분에 <ResponseEntity<?>> 명시
                .orElseGet(() -> ResponseEntity
                        .status(404)
                        .body(Map.of("error", "해당 사용자를 찾을 수 없습니다.")));
    }


    @PutMapping("/users/status")
    public ResponseEntity<?> updateUserStatusByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        String email = payload.get("email");
        String status = payload.get("status");

        if (email == null || email.isBlank() || status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일과 상태를 모두 입력하세요."));
        }

        try {
            User updatedUser = adminService.updateUserStatusAsAdmin(email, status);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // 내부 권한 검증 메서드
    private boolean isAuthorizedAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7);
        return adminService.isValidAdminToken(token);
    }

    // 신고 당한 게시글 전체 조회 (관리자 전용)
    @GetMapping("/boards/reports")
    public ResponseEntity<?> getReportedBoards(@RequestHeader("Authorization") String authorizationHeader) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }
        return ResponseEntity.ok(boardReportService.getAllReports());
    }

    // 유저 경고 및 차단 API (관리자 전용)
    @PostMapping("/users/warning")
    public ResponseEntity<?> warnOrBlockUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        String email = payload.get("email");
        String status = payload.get("status"); // "경고" or "차단"
        String reason = payload.get("reason");

        if (email == null || email.isBlank()
                || status == null || (!status.equals("경고") && !status.equals("차단"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일과 올바른 status(경고 또는 차단)를 입력하세요."));
        }

        if (status.equals("경고") && userWarningService.isWarned(email)) {
            return ResponseEntity.ok(Map.of("message", "이미 경고된 사용자 입니다."));
        }

        userwarning userWarning = userWarningService.addWarningOrBlock(email, status, reason);

        if (status.equals("차단")) {
            userService.blockUser(email);
        }

        return ResponseEntity.ok(userWarning);
    }

    // 신고 처리 여부 수정 API
    @PutMapping("/boards/reports/{reportId}/resolve")
    public ResponseEntity<?> updateReportResolvedStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long reportId,
            @RequestBody Map<String, Boolean> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        Boolean resolved = payload.get("resolved");
        if (resolved == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "resolved 값은 필수입니다."));
        }

        try {
            BoardReport updatedReport = boardReportService.updateResolvedStatus(reportId, resolved);
            return ResponseEntity.ok(updatedReport);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // 경고된 사용자 및 사유 목록 조회
    @GetMapping("/users/warnings")
    public ResponseEntity<?> getWarnedUsers(@RequestHeader("Authorization") String authorizationHeader) {
        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }
        return ResponseEntity.ok(userWarningService.getWarningsByStatus("경고"));
    }

    // 차단된 사용자 및 사유 목록 조회
    @GetMapping("/users/blocks")
    public ResponseEntity<?> getBlockedUsersWarning() {
        return ResponseEntity.ok(userWarningService.getWarningsByStatus("차단"));
    }

    //학생 인증 유무 api
    @PutMapping("/users/certify")
    public ResponseEntity<?> updateStudentCertification(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, Object> payload) {

        if (!isAuthorizedAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 없습니다."));
        }

        String email = (String) payload.get("email");
        Object certifiedObj = payload.get("studentCertified");

        if (email == null || email.isBlank() || certifiedObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일과 studentCertified(true/false) 값을 입력하세요."));
        }

        boolean certified;
        try {
            certified = Boolean.parseBoolean(certifiedObj.toString()); // ✅ 문자열/boolean 둘 다 허용
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentCertified 값은 true/false 여야 합니다."));
        }

        try {
            User updatedUser = adminService.updateStudentCertification(email, certified);
            return ResponseEntity.ok(Map.of(
                    "email", updatedUser.getEmail(),
                    "studentCertified", updatedUser.isStudentCertified(),
                    "message", certified ? "학생 인증이 승인되었습니다." : "학생 인증이 해제되었습니다."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }


}


