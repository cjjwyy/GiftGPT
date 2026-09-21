package com.giftgpt.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.order.entity.FeedbackAccessToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FeedbackAccessTokenMapper extends BaseMapper<FeedbackAccessToken> {

    @Update("UPDATE feedback_access_token SET used = 1, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND used = 0 AND expire_at > CURRENT_TIMESTAMP")
    int consume(@Param("id") Long id);
}
