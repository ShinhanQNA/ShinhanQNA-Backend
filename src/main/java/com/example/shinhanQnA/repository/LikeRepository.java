package com.example.shinhanQnA.repository;


import com.example.shinhanQnA.entity.LikeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByPostIdAndUserEmail(Integer postId, String userEmail);
    void deleteByPostIdAndUserEmail(Integer postId, String userEmail);

    @Modifying
    @Transactional
    void deleteByPostId(Integer postId);

    @Modifying
    @Transactional
    void deleteAllByPostIdIn(List<Integer> postIds);

    @Modifying
    @Transactional
    void deleteAllByUserEmail(String userEmail);
}
