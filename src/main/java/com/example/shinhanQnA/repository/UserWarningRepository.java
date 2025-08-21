package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.userwarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWarningRepository extends JpaRepository<userwarning, Long> {
    boolean existsByEmailAndStatus(String email, String status);
    List<userwarning> findByStatus(String status);
    Optional<userwarning> findTopByEmailOrderByWarningDateDesc(String email);
}

