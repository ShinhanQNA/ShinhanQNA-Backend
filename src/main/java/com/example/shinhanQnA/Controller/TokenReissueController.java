package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.entity.Admin;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.AdminRepository;
import com.example.shinhanQnA.repository.UserRepository;
import com.example.shinhanQnA.service.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TokenReissueController {

    private static final Logger logger = LoggerFactory.getLogger(TokenReissueController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    @PostMapping("/token/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        logger.info("토큰 재발급 요청 - 받은 리프레시 토큰: {}", refreshToken);

        // 1. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            logger.warn("리프레시 토큰 유효하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 토큰에서 사용자 ID 추출
        String subjectId = jwtTokenProvider.getEmailFromToken(refreshToken);
        logger.info("리프레시 토큰에서 추출한 subjectId: {}", subjectId);

        // 3. Admin 먼저 조회
        Optional<Admin> adminOpt = adminRepository.findById(subjectId);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            String savedRefreshToken = admin.getRefreshToken();
            if (!refreshToken.equals(savedRefreshToken)) {
                logger.warn("저장된 리프레시 토큰과 요청 토큰 불일치 (adminId: {})", subjectId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 새 토큰 발급
            String newAccessToken = jwtTokenProvider.createAccessToken(subjectId);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(subjectId);
            int newExpiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();

            // DB에 새 refresh token 저장
            admin.setRefreshToken(newRefreshToken);
            adminRepository.save(admin);

            logger.info("어드민 토큰 재발급 완료 (id: {})", subjectId);
            return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken, newExpiresIn));
        }

        // 4. User 조회
        Optional<User> userOpt = userRepository.findByEmail(subjectId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String savedRefreshToken = user.getToken();
            if (!refreshToken.equals(savedRefreshToken)) {
                logger.warn("저장된 리프레시 토큰과 요청 토큰 불일치 (email: {})", subjectId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 새 토큰 발급
            String newAccessToken = jwtTokenProvider.createAccessToken(subjectId);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(subjectId);
            int newExpiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();

            user.setToken(newRefreshToken);
            userRepository.save(user);

            logger.info("유저 토큰 재발급 완료 (email: {})", subjectId);
            return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken, newExpiresIn));
        }

        // 5. 둘 다 없으면
        logger.warn("저장소에 리프레시 토큰이 존재하지 않음 (subjectId: {})", subjectId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

@Data
class TokenRequest {
    private String refreshToken;
}
