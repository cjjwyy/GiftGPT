package com.giftgpt.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.mapper.GiftRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GiftMemoryService {

    private final GiftRecordMapper giftRecordMapper;

    public Page<GiftRecord> listHistory(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<GiftRecord> p = new Page<>(page, size);
        return giftRecordMapper.selectPage(p,
                new LambdaQueryWrapper<GiftRecord>()
                        .eq(GiftRecord::getUserId, userId)
                        .orderByDesc(GiftRecord::getCreateTime));
    }

    public GiftRecord getById(Long id) {
        return giftRecordMapper.selectById(id);
    }
}
