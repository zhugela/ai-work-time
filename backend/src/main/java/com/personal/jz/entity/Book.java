package com.personal.jz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String currency;
    private String icon;
    private String status;  // ACTIVE / ARCHIVED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
