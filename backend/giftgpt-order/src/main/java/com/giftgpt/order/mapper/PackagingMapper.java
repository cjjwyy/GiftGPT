package com.giftgpt.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.order.entity.Packaging;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PackagingMapper extends BaseMapper<Packaging> {
    @org.apache.ibatis.annotations.Select("SELECT id FROM user WHERE id = #{id} FOR UPDATE")
    Long lockOwner(@org.apache.ibatis.annotations.Param("id") Long id);
}
