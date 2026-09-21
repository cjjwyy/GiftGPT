package com.giftgpt.content.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class CalendarEventRequest {
    private Long recipientId;

    @NotBlank(message = "事件标题不能为空")
    @Size(max = 100, message = "事件标题不能超过100个字符")
    private String title;

    @Size(max = 50)
    private String occasion;

    @NotNull(message = "事件日期不能为空")
    private LocalDate eventDate;

    @Min(value = 0, message = "提醒天数不能小于0")
    @Max(value = 365, message = "提醒天数不能超过365")
    private Integer remindBeforeDays;

    @Min(0)
    @Max(1)
    private Integer isRepeat;
}
