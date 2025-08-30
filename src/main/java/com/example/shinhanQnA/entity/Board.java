package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Board")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Integer postId;

    @Column(name = "Title")
    private String title;

    @Column(name = "Content")
    private String content;

    @Column(name = "Likes")
    private Integer likes = 0;

    @Column(name = "Date")
    private LocalDateTime date;

    @Column(name = "Image_path")
    private String imagePath;

    @Column(name = "Status")
    private String status; // "응답 대기", "응답 완료" 등

    @Column(name = "Email")
    private String email; // 작성자 이메일

    @Column(name = "year")
    private String year;
}
