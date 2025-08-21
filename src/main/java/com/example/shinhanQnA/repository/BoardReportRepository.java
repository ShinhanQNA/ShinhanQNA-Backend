package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.BoardReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardReportRepository extends JpaRepository<BoardReport, Long> {
    Long countByPostId(Integer postId);
}

