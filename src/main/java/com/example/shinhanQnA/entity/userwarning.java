package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "userwarning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class userwarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WarningID")
    private Long warningId;

    @Column(name = "Email", length = 255, nullable = false)
    private String email; // 유저 이메일 (경고 받는 사람)

    @Column(name = "Reason", length = 512)
    private String reason; // 경고 사유

    @Column(name = "warning_date")
    private java.time.LocalDateTime warningDate;

    @Column(name = "Status", length = 50)
    private String status; // 예: "경고", "차단"
}