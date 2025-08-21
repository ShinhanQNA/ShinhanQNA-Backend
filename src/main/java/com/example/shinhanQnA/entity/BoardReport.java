package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boardreport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Long reportId;

    @Column(name = "PostID", nullable = false)
    private Integer postId;

    @Column(name = "reporter_email", length = 255, nullable = false)
    private String reporterEmail; // 신고자 이메일

    @Column(name = "report_reason", length = 512)
    private String reportReason;

    @Column(name = "report_date")
    private java.time.LocalDateTime reportDate;

    @Column(name = "Resolved", nullable = false)
    private boolean resolved; // 신고 처리 여부 (true/false)
}
