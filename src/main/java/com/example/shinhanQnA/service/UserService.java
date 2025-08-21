package com.example.shinhanQnA.service;


import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public User certifyStudent(String email, Integer students, String name, String department, Integer year, String role, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));


        // S3에 이미지 업로드
        String imagePath = s3Uploader.upload(image, "student-card");

        // 사용자 정보 업데이트 (필요시 students 및 기타 필드 추가)
        user.setName(name);
        user.setDepartment(department);
        user.setYear(year != null ? year.toString() : null);
        user.setStudentCardImagePath(imagePath);
        user.setRole(role);
        user.setStudents(students != null ? students.toString() : null);


        return userRepository.save(user);
    }

    // 사용자 정보 + 가입 상태 조회
    public User getUserInfo(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
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

    }
