package com.example.shinhanQnA.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BoardResponseDTO {
    private Integer postId;
    private String title;
    private String content;
    private Integer likes;
    private LocalDateTime date;
    private String status;
    private Long reportCount;
    private String warningStatus;
    private String writerEmail;


    private String imagePath;
}


