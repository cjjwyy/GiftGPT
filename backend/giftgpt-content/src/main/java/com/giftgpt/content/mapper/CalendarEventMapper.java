package com.giftgpt.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.content.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CalendarEventMapper extends BaseMapper<CalendarEvent> {
}
