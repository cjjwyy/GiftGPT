package com.giftgpt.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.mapper.GiftRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GiftMemoryService {

    private final GiftRecordMapper giftRecordMapper;

    public Page<GiftRecord> listHistory(int page, int size, Long recipientId, String occasion, String status) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<GiftRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<GiftRecord> w = new LambdaQueryWrapper<GiftRecord>()
                .eq(GiftRecord::getUserId, userId)
                .orderByDesc(GiftRecord::getCreateTime);
        if (recipientId != null) w.eq(GiftRecord::getRecipientId, recipientId);
        if (occasion != null && !occasion.isBlank()) w.eq(GiftRecord::getOccasion, occasion);
        if (status != null && !status.isBlank()) w.eq(GiftRecord::getStatus, status);
        return giftRecordMapper.selectPage(p, w);
    }

    public GiftRecord getById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        GiftRecord record = giftRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return record;
    }
}
