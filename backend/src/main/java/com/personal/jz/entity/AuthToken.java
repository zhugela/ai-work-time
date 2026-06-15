package com.personal.jz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_token")
public class AuthToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Integer revoked;
}
