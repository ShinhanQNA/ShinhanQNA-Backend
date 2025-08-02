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
    private Integer department;

    @Column(name = "studentCardImagePath", length = 255)
    private String studentCardImagePath;

    @Column(name = "Field", length = 255)
    private String field;

    // 필요 시 toString, equals, hashCode 등 추가
}
