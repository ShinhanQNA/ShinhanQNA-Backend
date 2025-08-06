package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`User`")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "Email", length = 255)
    private String email;

    @Column(name = "Name", length = 255)
    private String name;

    @Column(name = "Token", length = 255)
    private String token;

    @Column(name = "Role", length = 255)
    private String role;

    @Column(name = "Year", length = 255)
    private String year;

    @Column(name = "Department")
    private String department;

    @Column(name = "student_card_image_path", length = 255)
    private String studentCardImagePath;

    @Column(name = "Students", length = 255) // 혹은 적절한 컬럼명과 타입
    private String students;



}
