package com.giftgpt.content.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("story_reply")
public class StoryReply extends BaseEntity {

    private Long storyId;
    private Long userId;
    private String content;

    @TableField(exist = false)
    private String nickname;
}
