package com.giftgpt.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback_access_token")
public class FeedbackAccessToken extends BaseEntity {
    private Long giftRecordId;
    private String tokenHash;
    private LocalDateTime expireAt;
    private Integer used;
}
