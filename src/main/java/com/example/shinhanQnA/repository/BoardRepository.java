package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Integer> {
    List<Board> findByStatus(String status);
    List<Board> findByEmail(String email);
    // 최신 날짜 기준 내림차순으로 전체 게시글 조회
    List<Board> findAllByOrderByDateDesc();
}
