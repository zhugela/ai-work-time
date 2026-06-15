package com.personal.jz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String ip;
    private String userAgent;
    private String result;
    private String detail;
    private LocalDateTime createdAt;
}
