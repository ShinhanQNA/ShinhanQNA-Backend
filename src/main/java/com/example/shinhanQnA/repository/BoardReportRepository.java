package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.BoardReport;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface BoardReportRepository extends JpaRepository<BoardReport, Long> {
    Long countByPostId(Integer postId);
    boolean existsByPostIdAndReporterEmail(Integer postId, String reporterEmail);

    @Modifying
    @Transactional
    void deleteAllByPostIdIn(List<Integer> postIds);

    @Modifying
    @Transactional
    void deleteAllByReporterEmail(String reporterEmail);
}

