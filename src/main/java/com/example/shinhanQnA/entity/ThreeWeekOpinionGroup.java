package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "three_week_opinion_groups",
        uniqueConstraints = @UniqueConstraint(columnNames = {"selected_year", "selected_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreeWeekOpinionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "selected_year", nullable = false)
    private Integer selectedYear;

    @Column(name = "selected_month", nullable = false)
    private Integer selectedMonth;

    @Column(name = "response_status", length = 20, nullable = false)
    private String responseStatus = "응답 대기";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThreeWeekOpinion> opinions;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

