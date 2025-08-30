package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.TokenResponse;
import com.example.shinhanQnA.entity.Admin;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.AdminRepository;
import com.example.shinhanQnA.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final AdminRepository adminRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public Admin createAdmin(String id, String rawPassword, String name) {
        if (adminRepository.findById(id).isPresent()) {
            logger.warn("이미 존재하는 관리자 id 요청: {}", id);
            throw new RuntimeException("이미 존재하는 관리자 계정입니다.");
        }
        Admin admin = Admin.builder()
                .id(id)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role("ADMIN")
                .status("ACTIVE")
                .build();

        Admin savedAdmin = adminRepository.save(admin);
        logger.info("관리자 계정 생성 성공: id={}", id);
        return savedAdmin;
    }

    @Transactional
    public TokenResponse loginAdmin(String id, String rawPassword) {
        logger.info("관리자 로그인 시도: id={}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("관리자 계정을 찾을 수 없음: id={}", id);
                    return new RuntimeException("관리자 계정을 찾을 수 없습니다.");
                });

        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            logger.warn("비밀번호가 일치하지 않음: id={}", id);
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        if (!"ACTIVE".equals(admin.getStatus())) {
            logger.warn("비활성화된 계정: id={}, status={}", id, admin.getStatus());
            throw new RuntimeException("비활성화된 관리자 계정입니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(admin.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId());
        int expiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();


        admin.setRefreshToken(refreshToken);
        adminRepository.save(admin);
        logger.info("RefreshToken DB 저장 완료: {}", refreshToken);

        logger.info("관리자 로그인 성공: id={}, 토큰 발급 및 Refresh Token 저장 완료", id);

        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }

    @Transactional
    public TokenResponse reissueAccessToken(String refreshToken) {
        logger.info("Access Token 재발급 요청");


        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token 입니다.");
        }

        String adminId = jwtTokenProvider.getEmailFromToken(refreshToken);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자 계정을 찾을 수 없습니다."));


        if (!refreshToken.equals(admin.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 정보가 일치하지 않습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(adminId);
        int expiresIn = jwtTokenProvider.getAccessTokenValidTimeSeconds();

        logger.info("Access Token 재발급 성공: id={}", adminId);

        return new TokenResponse(newAccessToken, refreshToken, expiresIn);
    }

    @Transactional
    public User updateUserStatusAsAdmin(String email, String newStatus) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자를 찾을 수 없습니다."));
        user.setStatus(newStatus);
        return userRepository.save(user);
    }

    public boolean isValidAdminToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        String adminId = jwtTokenProvider.getEmailFromToken(token);
        return adminRepository.findById(adminId)
                .map(admin -> "ADMIN".equals(admin.getRole()))
                .orElse(false);
    }

    // 학생 인증 유무
    public User approveStudentCertification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        if (user.isStudentCertified()) {
            throw new RuntimeException("이미 학생 인증이 완료된 사용자입니다.");
        }

        user.setStudentCertified(true);
        return userRepository.save(user);
    }

    public User updateStudentCertification(String email, boolean certified) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        user.setStudentCertified(certified);
        return userRepository.save(user);
    }




}