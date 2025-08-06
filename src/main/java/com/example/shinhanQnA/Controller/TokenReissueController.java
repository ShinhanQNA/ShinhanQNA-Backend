package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.UserRepository;
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
    private final UserRepository userRepository;

    @PostMapping("/token/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        logger.info("토큰 재발급 요청 - 받은 리프레시 토큰: {}", refreshToken);

        // 1. 리프레시 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            logger.warn("리프레시 토큰 유효하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getEmailFromToken(refreshToken);
        logger.info("리프레시 토큰에서 추출한 userId: {}", userId);

        // 3. 저장소에 등록된 토큰과 비교
        Optional<User> userOpt = userRepository.findByEmail(userId);
        if (userOpt.isEmpty()) {
            logger.warn("저장소에 리프레시 토큰이 존재하지 않음 (userId: {})", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        String savedRefreshToken = user.getToken();

        if (!savedRefreshToken.equals(refreshToken)) {
            logger.warn("저장된 리프레시 토큰과 요청 토큰 불일치 (userId: {})", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("리프레시 토큰 검증 통과 (userId: {})", userId);

        // 4. 새로운 액세스 토큰, 리프레시 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        int newExpiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();

        // 5. 새로운 리프레시 토큰 저장
        user.setToken(newRefreshToken);
        userRepository.save(user);
        logger.info("새 리프레시 토큰 저장 완료 (userId: {})", userId);

        // 6. 새 토큰 반환
        logger.info("새 토큰 응답 생성 완료 (userId: {})", userId);
        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken, newExpiresIn));
    }

}

@Data
class TokenRequest {
    private String refreshToken;
}
