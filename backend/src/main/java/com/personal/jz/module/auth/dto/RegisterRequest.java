package com.personal.jz.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅含字母数字下划线")
    private String username;

    @Size(max = 16)
    private String nickname;

    @NotBlank
    @Email
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 32)
    @Pattern(regexp = "(?=.*[A-Za-z])(?=.*\\d).{8,}", message = "密码必须 8-32 位且含字母与数字")
    private String password;
}
