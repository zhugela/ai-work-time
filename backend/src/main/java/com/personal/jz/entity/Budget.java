package com.personal.jz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private Long categoryId;
    private String yearMonth;
    private BigDecimal amount;
    private BigDecimal warnThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
