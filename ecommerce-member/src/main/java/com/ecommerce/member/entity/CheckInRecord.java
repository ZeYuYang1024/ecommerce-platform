package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("check_in_record")
public class CheckInRecord {
    private Long id;
    private Long userId;
    private LocalDate checkDate;
    private Integer consecutiveDays;
    private Integer pointsAwarded;
    private String bizKey;
    private LocalDateTime createdAt;
}
