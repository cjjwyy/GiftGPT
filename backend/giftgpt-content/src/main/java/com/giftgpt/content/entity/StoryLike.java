package com.giftgpt.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("story_like")
public class StoryLike extends BaseEntity {

    private Long storyId;
    private Long userId;
}
