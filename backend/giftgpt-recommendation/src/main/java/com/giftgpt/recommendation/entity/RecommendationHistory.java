package com.giftgpt.recommendation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation_history")
public class RecommendationHistory extends BaseEntity {

    private Long userId;
    private Long recipientId;
    private String scene;
    private BigDecimal budget;
    private String result;
    private String feedback;
}
