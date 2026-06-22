package com.giftgpt.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.content.entity.StoryReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoryReplyMapper extends BaseMapper<StoryReply> {

    @Select("SELECT r.id, r.story_id, r.user_id, r.content, r.create_time, r.update_time, u.nickname " +
            "FROM story_reply r LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.story_id = #{storyId} ORDER BY r.create_time ASC")
    List<StoryReply> selectRepliesWithUser(@Param("storyId") Long storyId);
}
