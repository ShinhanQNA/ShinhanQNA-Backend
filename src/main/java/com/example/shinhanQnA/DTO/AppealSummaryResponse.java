package com.example.shinhanQnA.DTO;

import com.example.shinhanQnA.entity.Appeal;
import com.example.shinhanQnA.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record AppealSummaryResponse(
        Long id,
        String email,
        String name,
        String students,
        String year,
        String department,
        LocalDateTime createdAt
) {
    public static AppealSummaryResponse fromEntity(Appeal appeal, User user) {
        return new AppealSummaryResponse(
                appeal.getId(),
                appeal.getEmail(),
                user.getName(),
                user.getStudents(),
                user.getYear(),
                user.getDepartment(),
                appeal.getCreatedAt()
        );
    }
}




