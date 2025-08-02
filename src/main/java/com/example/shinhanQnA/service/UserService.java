//package com.example.shinhanQnA.service;
//
//import com.example.shinhanQnA.entity.User;
//import com.example.shinhanQnA.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final S3Uploader s3Uploader; // S3 업로더 서비스
//
//    @Transactional
//    public User certifyStudent(String email, Integer students, String name, String department, Integer year, MultipartFile image) {
//        User user = userRepository.findById(email)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
//
//        // S3에 이미지 업로드
//        String imagePath = s3Uploader.upload(image, "student-card");
//
//        // 사용자 정보 업데이트
//        user.setName(name);
//        user.setDepartment(department);
//        user.setYear(year != null ? year.toString() : null);
//        user.setStudentCardImagePath(imagePath);
//
//        return userRepository.save(user);
//    }
//}
