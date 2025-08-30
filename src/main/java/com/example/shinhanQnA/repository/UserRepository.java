package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findAllByStatus(String status);
    List<User> findAllByStatusOrderByCreatedAtDesc(String status);
}
