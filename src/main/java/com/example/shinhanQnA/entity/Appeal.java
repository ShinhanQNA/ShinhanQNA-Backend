package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeal")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;  // 이의제기한 사용자 계정

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false)
    private String status; // "대기", "승인", "거절" 상태 관리
}

