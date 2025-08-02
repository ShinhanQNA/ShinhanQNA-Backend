package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.StudentCertificationRequest;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping(value = "/certify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> certifyStudent(
            @RequestHeader("Authorization") String accessToken,
            @RequestPart("students") Integer students,
            @RequestPart("name") String name,
            @RequestPart("department") String department,
            @RequestPart("year") Integer year,
            @RequestPart("image") MultipartFile image
    ) {
        // "Bearer " 제거
        String token = accessToken.replace("Bearer ", "").trim();

        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 유효하지 않음");
        }

        String email = jwtTokenProvider.getUserIdFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자");
        }

        User updatedUser = userService.certifyStudent(email, students, name, department, year, image);

        return ResponseEntity.ok(updatedUser);
    }
}
