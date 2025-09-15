package com.example.shinhanQnA.DTO;

import com.example.shinhanQnA.entity.Appeal;
import com.example.shinhanQnA.entity.User;

import java.util.List;

public record AppealDetailResponse(
        Long id,
        String email,
        String name,
        String students,
        String year,
        String department,
        List<?> boards
) {
    public static AppealDetailResponse of(Appeal appeal, User user, List<?> boards) {
        return new AppealDetailResponse(
                appeal.getId(),
                user.getEmail(),
                user.getName(),
                user.getStudents(),
                user.getYear(),
                user.getDepartment(),
                boards
        );
    }
}
