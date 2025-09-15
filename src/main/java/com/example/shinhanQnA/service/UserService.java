package com.example.shinhanQnA.service;


import com.example.shinhanQnA.Controller.OauthController;
import com.example.shinhanQnA.DTO.PendingUserDetailResponse;
import com.example.shinhanQnA.DTO.PendingUserSummaryResponse;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final UserWarningRepository userWarningRepository;
    private final BoardRepository boardRepository;
    private final BoardReportRepository boardReportRepository;
    private final LikeRepository likeRepository;

    private static final Logger logger = LoggerFactory.getLogger(OauthController.class);

    @Transactional
    public User certifyStudent(String email, Integer students, String name, String department, Integer year, String role, Boolean studentCertified, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        // S3에 이미지 업로드
        String imagePath = s3Uploader.upload(image, "student-card");

        // 사용자 정보 업데이트
        user.setName(name);
        user.setDepartment(department);
        user.setYear(year != null ? year.toString() : null);
        user.setStudentCardImagePath(imagePath);
        user.setRole(role);
        user.setStudents(students != null ? students.toString() : null);

        // studentCertified 값 반영 (null 체크 포함)
        if (studentCertified != null) {
            user.setStudentCertified(studentCertified);
        }

        return userRepository.save(user);
    }

    // 사용자 정보 + 가입 상태 조회
    public User getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        logger.info("[UserService] 조회 사용자 상태: {}", user.getStatus());
        return user;
    }


    // 가입 상태 변경 및 저장
    @Transactional
    public User updateUserStatus(String email, String newStatus) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        user.setStatus(newStatus);
        return userRepository.save(user);
    }

    @Transactional
    public void blockUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));
        user.setStatus("차단"); // 차단 상태 설정
        userRepository.save(user);
    }

    @Transactional
    public void deleteUserWithRelatedData(String email) {
        // 1. 해당 유저 관련 userwarning 삭제
        userWarningRepository.deleteAllByEmail(email);

        // 2. 해당 유저가 작성한 게시글 관련 신고 삭제
        List<Integer> postIds = boardRepository.findPostIdsByEmail(email);
        if (!postIds.isEmpty()) {
            boardReportRepository.deleteAllByPostIdIn(postIds);
            likeRepository.deleteAllByPostIdIn(postIds);
        }

        // 3. 해당 유저의 게시글 삭제
        boardRepository.deleteAllByEmail(email);

        // 4. 해당 유저가 신고한 신고 내역 삭제 (선택사항)
        boardReportRepository.deleteAllByReporterEmail(email);

        // 5. 해당 유저가 공감한 내역 삭제
        likeRepository.deleteAllByUserEmail(email);

        // 6. 유저 정보 삭제
        userRepository.deleteById(email);
    }

//    // 가입 대기 사용자 전체 조회 (최신순)
//    public List<PendingUserSummaryResponse> getPendingUsersSummary() {
//        List<User> pendingUsers = userRepository.findAllByStatusOrderByCreatedAtDesc("가입 대기 중");
//        return pendingUsers.stream()
//                .map(PendingUserSummaryResponse::fromEntity)
//                .toList();
//    }
//
//    // 가입 대기 사용자 상세 조회
//    public PendingUserDetailResponse getPendingUserDetail(String email) {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다: " + email));
//        if (!"가입 대기 중".equals(user.getStatus())) {
//            throw new RuntimeException("해당 사용자는 가입 대기 상태가 아닙니다.");
//        }
//        return PendingUserDetailResponse.fromEntity(user);
//    }

    // UserService.java 주요 부분 예시
    public List<PendingUserSummaryResponse> getPendingUsersSummary() {
        return userRepository.findByStudentCertifiedTrue().stream()
                .filter(user -> "가입 대기 중".equals(user.getStatus()))
                .map(user -> new PendingUserSummaryResponse(
                        user.getEmail(),
                        user.getName(),
                        user.getStudents(),
                        user.getYear(),
                        user.getDepartment()
                ))
                .collect(Collectors.toList());
    }

    public PendingUserDetailResponse getPendingUserDetail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        if (!user.isStudentCertified() || !"가입 대기 중".equals(user.getStatus())) {
            throw new RuntimeException("인증된 가입 대기 유저가 아닙니다.");
        }
        return new PendingUserDetailResponse(
                user.getEmail(),
                user.getName(),
                user.getStudents(),
                user.getYear(),
                user.getDepartment(),
                user.getStudentCardImagePath()
        );
    }


    @Transactional
    public User saveUser(User user) {
        logger.info("[UserService] 저장 전 상태: {}", user.getStatus());
        User savedUser = userRepository.save(user);
        logger.info("[UserService] 저장 후 상태: {}", savedUser.getStatus());
        return savedUser;
    }




}
