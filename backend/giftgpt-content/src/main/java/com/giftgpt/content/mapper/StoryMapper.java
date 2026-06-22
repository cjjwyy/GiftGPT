package com.giftgpt.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.content.entity.Story;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StoryMapper extends BaseMapper<Story> {

    @Select("SELECT s.id, s.user_id, s.gift_record_id, s.title, s.content, s.images, " +
            "s.likes, s.is_anonymous, s.status, s.create_time, s.update_time, " +
            "u.nickname, " +
            "(CASE WHEN sl.id IS NOT NULL THEN 1 ELSE 0 END) AS liked " +
            "FROM story s " +
            "LEFT JOIN user u ON s.user_id = u.id " +
            "LEFT JOIN story_like sl ON sl.story_id = s.id AND sl.user_id = #{currentUserId} " +
            "WHERE s.status = 1 " +
            "ORDER BY s.create_time DESC")
    Page<Story> selectPageWithUser(Page<Story> page, @Param("currentUserId") Long currentUserId);
}
