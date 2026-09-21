package com.giftgpt.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommend_event")
public class RecommendEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long recipientId;
    private String occasion;
    private Long productId;
    private String productName;
    private String eventType;
    private LocalDateTime createTime;
}
