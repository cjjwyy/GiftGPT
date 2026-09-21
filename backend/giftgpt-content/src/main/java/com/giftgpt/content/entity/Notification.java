package com.giftgpt.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {
    private Long userId;
    private Long calendarEventId;
    private LocalDate occurrenceDate;
    private String title;
    private String content;
    private Integer isRead;
}
