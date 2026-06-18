package com.giftgpt.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String authProvider;
    private String openId;
    private Integer status;
}
