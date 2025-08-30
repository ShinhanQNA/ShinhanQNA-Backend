package com.example.shinhanQnA.DTO;

import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.entity.userwarning;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserWithWarningsResponse {
    private User user;
    private List<userwarning> warnings;
}

