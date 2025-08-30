package com.example.shinhanQnA.repository;

import com.example.shinhanQnA.entity.ThreeWeekOpinionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThreeWeekOpinionGroupRepository extends JpaRepository<ThreeWeekOpinionGroup, Long> {
    Optional<ThreeWeekOpinionGroup> findBySelectedYearAndSelectedMonth(Integer year, Integer month);
    List<ThreeWeekOpinionGroup> findAllBySelectedYear(Integer selectedYear);
}

