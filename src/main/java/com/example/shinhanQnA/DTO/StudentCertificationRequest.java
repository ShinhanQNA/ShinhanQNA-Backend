package com.example.shinhanQnA.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class StudentCertificationRequest {
    private Integer studentid;
    private String name;
    private String department;
    private Integer year;
    private MultipartFile image;   // 학생증 이미지 파일
}

