package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.userwarning;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface UserWarningRepository extends JpaRepository<userwarning, Long> {
    boolean existsByEmailAndStatus(String email, String status);
    List<userwarning> findByStatus(String status);
    Optional<userwarning> findTopByEmailOrderByWarningDateDesc(String email);
    List<userwarning> findByEmail(String email);
    @Modifying
    @Transactional
    void deleteAllByEmail(String email);
}

