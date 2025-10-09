package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.BoardReport;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 특정 사용자가 작성한 게시글 중 신고받은 게시글 ID 목록 조회
    @Query("SELECT DISTINCT br.postId FROM BoardReport br JOIN Board b ON br.postId = b.postId WHERE b.email = :email")
    List<Integer> findReportedPostIdsByWriterEmail(@Param("email") String email);

    // 특정 게시글 ID 목록에 대한 모든 신고 삭제
    @Modifying
    @Transactional
    void deleteByPostIdIn(List<Integer> postIds);
}
