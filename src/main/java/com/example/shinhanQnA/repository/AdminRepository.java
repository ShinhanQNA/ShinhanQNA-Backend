package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findById(String id);
}

