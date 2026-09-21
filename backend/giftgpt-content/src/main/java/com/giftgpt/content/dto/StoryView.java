package com.giftgpt.content.dto;

import com.giftgpt.content.entity.Story;
import lombok.Data;
import java.time.LocalDateTime;

/** Public response deliberately excludes private giftRecordId and internal status. */
@Data
public class StoryView {
    private Long id;
    private Long userId;
    private String nickname;
    private String title;
    private String content;
    private String images;
    private Integer likes;
    private Integer liked;
    private Integer isAnonymous;
    private LocalDateTime createTime;
    private boolean canDelete;

    public static StoryView from(Story story, Long viewer) {
        StoryView view = new StoryView();
        org.springframework.beans.BeanUtils.copyProperties(story, view);
        view.setCanDelete(viewer != null && viewer.equals(story.getUserId()));
        if (Integer.valueOf(1).equals(story.getIsAnonymous())) {
            view.setUserId(null);
            view.setNickname(null);
        }
        return view;
    }
}
