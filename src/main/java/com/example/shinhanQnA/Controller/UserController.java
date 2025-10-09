package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.PendingUserDetailResponse;
import com.example.shinhanQnA.DTO.PendingUserSummaryResponse;
import com.example.shinhanQnA.DTO.UserWithWarningsResponse;
import com.example.shinhanQnA.entity.Admin;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.entity.userwarning;
import com.example.shinhanQnA.service.AdminService;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.UserService;
import com.example.shinhanQnA.repository.UserRepository;
import com.example.shinhanQnA.service.UserWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserWarningService userWarningService;
    private final AdminService adminService;

    // 토큰 검증 및 이메일 추출 (예외 발생 시 RuntimeException 던짐)
    private String validateAndExtractEmail(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 올바르지 않습니다. Bearer 스킴 필요");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        return jwtTokenProvider.getEmailFromToken(token);
    }

    // 2차 학생 인증 처리 (multipart/form-data)
    @PostMapping(value = "/certify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> certifyStudent(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam("students") Integer students,
            @RequestParam("name") String name,
            @RequestParam("department") String department,
            @RequestParam("year") Integer year,
            @RequestParam("role") String role,
            @RequestParam("studentCertified") Boolean studentCertified,
            @RequestPart("image") MultipartFile image) {

        try {
            String token = accessToken.replace("Bearer ", "").trim();
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 유효하지 않음");
            }

            String email = jwtTokenProvider.getEmailFromToken(token);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자");
            }

            userService.certifyStudent(email, students, name, department, year, role, studentCertified, image); // 인자 추가
            Map<String, String> response = Map.of("message", "성공");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "인증 실패", "message", e.getMessage()));
        }
    }

    // 사용자 로그아웃 처리 (토큰 초기화)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String accessToken) {
        String token = accessToken.replace("Bearer ", "").trim();
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 유효하지 않음");
        }

        String email = jwtTokenProvider.getEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setToken(null);
            userRepository.save(user);
        });

        Map<String, String> response = Map.of("message", "성공");
        return ResponseEntity.ok(response);
    }

    // 사용자 정보 및 가입 상태 조회 API - GET /users/me
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String email = validateAndExtractEmail(authorizationHeader);

            // 관리자 토큰인지 확인
            String token = authorizationHeader.substring(7);
            if (adminService.isValidAdminToken(token)) {
                // 관리자 정보 반환
                Admin admin = adminService.getAdminInfo(email);
                return ResponseEntity.ok(admin);
            } else {
                // 일반 사용자 정보 반환
                User user = userService.getUserInfo(email);
                List<userwarning> warnings = userWarningService.getWarningsByEmail(email);
                return ResponseEntity.ok(new UserWithWarningsResponse(user, warnings));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증 실패", "message", e.getMessage()));
        }
    }

    // 가입 상태 변경 API - PUT /users/me/status
    @PutMapping("/me/status")
    public ResponseEntity<?> updateUserStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> payload) {

        try {
            String email = validateAndExtractEmail(authorizationHeader);

            String newStatus = payload.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "잘못된 요청", "message", "상태 값을 입력하세요."));
            }

            User updatedUser = userService.updateUserStatus(email, newStatus);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "상태 변경 실패", "message", e.getMessage()));
        }
    }

    // DELETE /users/me
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String email = validateAndExtractEmail(authorizationHeader);
            userService.deleteUserWithRelatedData(email);
            return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "회원 탈퇴 실패", "message", e.getMessage()));
        }
    }



}
