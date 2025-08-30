package com.example.shinhanQnA.DTO;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreeWeekOpinionGroupDTO {
    private Long id;
    private Integer selectedYear;
    private Integer selectedMonth;
    private String responseStatus;
    private LocalDateTime createdAt;
    private List<ThreeWeekOpinionDTO> opinions;
}
