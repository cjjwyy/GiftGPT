package com.giftgpt.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.recommendation.entity.RecommendationHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecommendationHistoryMapper extends BaseMapper<RecommendationHistory> {
}
