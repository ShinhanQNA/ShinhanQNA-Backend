package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    // 기본 CRUD 제공
}
