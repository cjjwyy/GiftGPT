package com.giftgpt.content.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("story_report")
public class StoryReport extends BaseEntity {
    private Long storyId;
    private Long reporterId;
    private String reason;
    private String detail;
    private String status;
    private Long moderatorId;
    private String decisionNote;
}
