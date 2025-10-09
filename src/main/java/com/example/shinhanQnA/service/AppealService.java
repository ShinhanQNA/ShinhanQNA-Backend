package com.example.shinhanQnA.service;

import com.example.shinhanQnA.entity.Appeal;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.AppealRepository;
import com.example.shinhanQnA.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealService {

    private static final Logger logger = LoggerFactory.getLogger(AppealService.class);

    private final AppealRepository appealRepository;
    private final UserRepository userRepository;
    private final UserWarningService userWarningService;

    // 사용자 이의제기 신청
    public Appeal submitAppeal(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        if (!"차단".equals(user.getStatus())) {
            throw new RuntimeException("차단된 사용자만 이의제기 할 수 있습니다.");
        }

        boolean alreadyExists = appealRepository.existsByEmailAndStatus(email, "대기");
        if (alreadyExists) {
            throw new RuntimeException("이미 제출한 이의제기가 처리 대기 중입니다.");
        }

        Appeal appeal = Appeal.builder()
                .email(email)
                .createdAt(LocalDateTime.now())
                .status("대기")
                .build();
        return appealRepository.save(appeal);
    }

    // 대기 상태인 이의제기만 조회 (관리자용)
    public List<Appeal> getAllPendingAppeals() {
        return appealRepository.findByStatus("대기");
    }

    // 특정 사용자 이의제기 조회 (모든 상태)
    public List<Appeal> getAppealsByEmail(String email) {
        return appealRepository.findByEmail(email);
    }

    /**
     * 이의제기 status 변경 및 유저 status 동기화 처리
     * @param appealId 이의제기 id
     * @param newStatus "승인" 또는 "거절"
     * @return 업데이트 된 Appeal 객체
     */
    @Transactional
    public Appeal updateAppealStatusAndSyncUser(Long appealId, String newStatus) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new RuntimeException("해당 이의제기를 찾을 수 없습니다."));

        if (!newStatus.equals("승인") && !newStatus.equals("거절")) {
            throw new RuntimeException("이의제기 상태는 '승인' 또는 '거절'만 가능합니다.");
        }

        User user = userRepository.findByEmail(appeal.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        if (newStatus.equals("승인")) {
            // 이의제기 승인시 차단 -> 가입 완료 상태로 변경
            if ("차단".equals(user.getStatus())) {
                user.setStatus("가입 완료");
                userRepository.save(user);

                // 해당 유저의 모든 경고/차단 정보 삭제
                userWarningService.deleteAllWarningsByEmail(appeal.getEmail());
                logger.info("이의제기 승인으로 인한 유저 {} 경고/차단 이력 삭제 완료", appeal.getEmail());

                // 이의제기 정보 삭제 (전체 이의제기 조회에서 제외)
                appealRepository.delete(appeal);
                logger.info("이의제기 승인으로 인한 appeal {} 삭제 완료", appealId);

                return appeal; // 삭제되기 전 정보 반환
            }
        } else if (newStatus.equals("거절")) {
            // 이의제기 거절시 차단 -> 완전 차단 상태로 변경
            if ("차단".equals(user.getStatus())) {
                user.setStatus("완전 차단");
                userRepository.save(user);
                logger.info("이의제기 거절으로 인한 유저 {} 상태 변경: 차단 -> 완전 차단", appeal.getEmail());
            }
            appeal.setStatus(newStatus);
            appealRepository.save(appeal);
        }

        return appeal;
    }

    public List<Appeal> getAllPendingAppealsSortedByDate() {
        // '대기' 상태 이의제기 중 createdAt 내림차순 정렬 조회
        return appealRepository.findByStatusOrderByCreatedAtDesc("대기");
    }

    public Appeal getAppealByEmail(String email) {
        return appealRepository.findByEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 이메일에 이의제기가 존재하지 않습니다."));
    }


}
