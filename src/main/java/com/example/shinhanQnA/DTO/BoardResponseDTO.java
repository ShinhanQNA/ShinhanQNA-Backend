package com.example.shinhanQnA.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardResponseDTO {
    private Integer postId;
    private String title;
    private String content;
    private Integer likes;
    private LocalDateTime date;
    private String status;         // 게시글 상태 (“응답 대기”, “응답 완료” 등)
    private Long reportCount;      // 해당 게시글 신고 횟수
    private String warningStatus;
    private String writerEmail;
    private String imagePath;
}

