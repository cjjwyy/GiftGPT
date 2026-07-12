package com.giftgpt.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistics_event")
public class LogisticsEvent {
    private Long id;
    private Long orderId;
    private LocalDateTime eventTime;
    private String location;
    private String status;
    private String description;
}
