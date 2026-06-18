package com.giftgpt.enterprise.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.enterprise.dto.BatchOrderRequest;
import com.giftgpt.enterprise.entity.Enterprise;
import com.giftgpt.enterprise.mapper.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;

    public Enterprise register(Enterprise enterprise) {
        Long userId = StpUtil.getLoginIdAsLong();
        enterprise.setUserId(userId);
        enterprise.setStatus("pending");
        enterprise.setSubscription("free");
        enterpriseMapper.insert(enterprise);
        return enterprise;
    }

    public Enterprise getById(Long id) {
        return enterpriseMapper.selectById(id);
    }

    public Enterprise getMyEnterprise() {
        Long userId = StpUtil.getLoginIdAsLong();
        return enterpriseMapper.selectOne(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getUserId, userId)
                        .eq(Enterprise::getStatus, "approved")
                        .last("limit 1"));
    }

    public Object createBatchOrder(BatchOrderRequest request) {
        return "批量下单已受理，共 " + request.getEmployees().size() + " 份礼物";
    }
}
