package com.example.shinhanQnA.DTO;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreeWeekOpinionDTO {
    private Long id;
    private Integer postId;
    private String title;
    private String content;
    private Integer likes;
    private LocalDateTime date;
    private String imagePath;
    private LocalDateTime createdAt;
}
