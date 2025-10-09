package com.example.shinhanQnA.service;

import com.example.shinhanQnA.entity.userwarning;
import com.example.shinhanQnA.repository.UserWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWarningService {

    private final UserWarningRepository userWarningRepository;

    public boolean isWarned(String email) {
        return userWarningRepository.existsByEmailAndStatus(email, "경고");
    }

    public userwarning addWarningOrBlock(String email, String status, String reason) {
        userwarning warning = userwarning.builder()
                .email(email)
                .status(status)
                .reason(reason)
                .warningDate(java.time.LocalDateTime.now())
                .build();
        return userWarningRepository.save(warning);
    }

    // 상태별(경고/차단) 조회
    public List<userwarning> getWarningsByStatus(String status) {
        return userWarningRepository.findByStatus(status);
    }

    public List<userwarning> getWarningsByEmail(String email) {
        return userWarningRepository.findByEmail(email);
    }

    // 특정 유저의 모든 경고/차단 정보 삭제 (이의제기 승인 시 사용)
    public void deleteAllWarningsByEmail(String email) {
        userWarningRepository.deleteAllByEmail(email);
    }
}
