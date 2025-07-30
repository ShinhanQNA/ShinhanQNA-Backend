package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.RefreshTokenRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TokenReissueController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/token/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenRequest request) {
        // 검증
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 저장소의 리프레시 토큰과 비교
        Optional<String> savedRefreshToken = refreshTokenRepository.findByUserId(userId);
        if (savedRefreshToken.isEmpty() || !savedRefreshToken.get().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(userId, newRefreshToken);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken, null, null));
    }
}

@Data
class TokenRequest {
    private String refreshToken;
}

