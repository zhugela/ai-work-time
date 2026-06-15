package com.personal.jz.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private Integer enabled;
    private Long currentBookId;
    private LocalDateTime createdAt;
}
