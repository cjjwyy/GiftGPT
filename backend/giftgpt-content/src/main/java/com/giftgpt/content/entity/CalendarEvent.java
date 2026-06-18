package com.giftgpt.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("calendar_event")
public class CalendarEvent extends BaseEntity {

    private Long userId;
    private Long recipientId;
    private String title;
    private String occasion;
    private LocalDate eventDate;
    private Integer remindBeforeDays;
    private Integer isRepeat;
}
