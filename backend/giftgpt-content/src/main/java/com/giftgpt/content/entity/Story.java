package com.giftgpt.content.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("story")
public class Story extends BaseEntity {

    private Long userId;
    private Long giftRecordId;
    private String title;
    private String content;
    private String images;
    private Integer likes;
    private Integer isAnonymous;
    private Integer status;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private Integer liked;
}

