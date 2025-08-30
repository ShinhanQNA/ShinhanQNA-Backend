package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "`User`")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "Email", length = 255)
    private String email;

    @Column(name = "Name", length = 255)
    private String name;

    @Column(name = "Token", length = 255)
    private String token;

    @Column(name = "Role", length = 255)
    private String role;

    @Column(name = "Year", length = 255)
    private String year;

    @Column(name = "Department")
    private String department;

    @Column(name = "student_card_image_path", length = 255)
    private String studentCardImagePath;

    @Column(name = "Students", length = 255)
    private String students;

    @Column(name = "status")
    private String status = "가입 대기 중";

    @Column(name = "student_certified", nullable = false)
    private boolean studentCertified = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }


}
