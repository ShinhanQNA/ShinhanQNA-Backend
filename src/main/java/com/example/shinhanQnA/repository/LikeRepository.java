package com.example.shinhanQnA.repository;


import com.example.shinhanQnA.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByPostIdAndUserEmail(Integer postId, String userEmail);
    void deleteByPostIdAndUserEmail(Integer postId, String userEmail);
}

