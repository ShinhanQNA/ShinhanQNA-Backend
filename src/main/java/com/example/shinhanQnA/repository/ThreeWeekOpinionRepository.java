package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.ThreeWeekOpinion;
import com.example.shinhanQnA.entity.ThreeWeekOpinionGroup;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreeWeekOpinionRepository extends JpaRepository<ThreeWeekOpinion, Long> {
    List<ThreeWeekOpinion> findByGroup(ThreeWeekOpinionGroup group, Sort sort);

    boolean existsByGroupAndBoard_PostId(ThreeWeekOpinionGroup group, Integer postId);
}

