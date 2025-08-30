package com.example.shinhanQnA.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupIdByMonthDTO {
    private Integer selectedMonth;
    private Long groupId;
    private String responseStatus;
}

