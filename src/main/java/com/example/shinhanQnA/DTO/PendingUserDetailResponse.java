package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingUserDetailResponse {
    private String email;
    private String name;
    private String students;
    private String year;
    private String department;
    private String imagePath;

    public static PendingUserDetailResponse fromEntity(com.example.shinhanQnA.entity.User user) {
        return new PendingUserDetailResponse(
                user.getEmail(),
                user.getName(),
                user.getStudents(),
                user.getYear(),
                user.getDepartment(),
                user.getStudentCardImagePath()
        );
    }
}

