package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingUserSummaryResponse {
    private String email;
    private String name;
    private String students;
    private String year;
    private String department;

    public static PendingUserSummaryResponse fromEntity(com.example.shinhanQnA.entity.User user) {
        return new PendingUserSummaryResponse(
                user.getEmail(),
                user.getName(),
                user.getStudents(),
                user.getYear(),
                user.getDepartment()
        );
    }
}

