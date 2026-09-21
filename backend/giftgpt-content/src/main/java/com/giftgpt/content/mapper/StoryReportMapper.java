package com.giftgpt.content.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.content.entity.StoryReport;
import org.apache.ibatis.annotations.*;
@Mapper
public interface StoryReportMapper extends BaseMapper<StoryReport> {
    @Select("SELECT * FROM story_report WHERE id = #{id} FOR UPDATE")
    StoryReport lockById(@Param("id") Long id);
}
