package com.example.shinhanQnA.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`admin`")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "status", length = 50)
    private String status = "ACTIVE";

    @Column(name = "token", length = 512)
    private String refreshToken;
}

