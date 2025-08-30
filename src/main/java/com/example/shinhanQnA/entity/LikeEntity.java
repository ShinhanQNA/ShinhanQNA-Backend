package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "like_table", uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_email"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;
}

