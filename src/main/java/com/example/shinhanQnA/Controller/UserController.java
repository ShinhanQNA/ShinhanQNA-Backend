package com.example.shinhanQnA.Controller;


import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.shinhanQnA.repository.UserRepository;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping(value = "/certify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> certifyStudent(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam("students") Integer students,
            @RequestParam("name") String name,
            @RequestParam("department") String department,
            @RequestParam("year") Integer year,
            @RequestParam("role") String role,
            @RequestPart("image") MultipartFile image
    ) {
        // 엑세스 토큰에서 Bearer 제거 및 유효성 검사
        String token = accessToken.replace("Bearer ", "").trim();
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 유효하지 않음");
        }

        // JWT의 subject(예: email) 추출
        String email = jwtTokenProvider.getEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자");
        }

        // 2차 학생 인증 처리 (S3 업로드 + DB 저장)
        User updatedUser = userService.certifyStudent(email, students, name, department, year, role,image);
        return ResponseEntity.ok("저장 완료");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken) {
        String token = accessToken.replace("Bearer ", "").trim();
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 유효하지 않음");
        }

        String email = jwtTokenProvider.getEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자");
        }

        // DB에서 해당 유저의 리프레시 토큰(= token 필드) 삭제/초기화
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setToken(null);
            userRepository.save(user);
        });

        return ResponseEntity.ok("로그아웃 완료");
    }
}
