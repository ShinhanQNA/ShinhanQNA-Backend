package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.Board;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Integer> {
    List<Board> findByStatus(String status);
    List<Board> findByEmail(String email);
    // 최신 날짜 기준 내림차순으로 전체 게시글 조회
    List<Board> findAllByOrderByDateDesc();
    List<Board> findByEmailOrderByDateDesc(String email);

    @Modifying
    @Transactional
    void deleteAllByEmail(String email);

    // 유저가 작성한 게시글(postId 리스트) 조회
    @Query("SELECT b.postId FROM Board b WHERE b.email = :email")
    List<Integer> findPostIdsByEmail(String email);
}
