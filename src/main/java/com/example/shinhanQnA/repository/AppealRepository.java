package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    boolean existsByEmailAndStatus(String email, String status); // 중복 방지 (대기 중인 appeal)
    List<Appeal> findByEmail(String email);
    List<Appeal> findByStatus(String status); // "대기" 상태만 조회 위해 추가
    List<Appeal> findByStatusOrderByCreatedAtDesc(String status);
}

