package com.giftgpt.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_authorization")
public class DataAuthorization extends BaseEntity {

    private Long userId;
    private String dataType;
    private String authorizedScope;
    private String status;
    private LocalDateTime expireAt;
}
