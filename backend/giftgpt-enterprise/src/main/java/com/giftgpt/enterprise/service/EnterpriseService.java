package com.giftgpt.enterprise.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.enterprise.dto.BatchOrderRequest;
import com.giftgpt.enterprise.dto.BatchOrderResponse;
import com.giftgpt.enterprise.dto.EnterpriseRegisterRequest;
import com.giftgpt.enterprise.entity.Enterprise;
import com.giftgpt.enterprise.mapper.EnterpriseMapper;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.mapper.OrderMapper;
import com.giftgpt.order.service.OrderService;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final RecipientMapper recipientMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public Enterprise register(EnterpriseRegisterRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Enterprise> existing = enterpriseMapper.selectList(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getUserId, userId)
                        .orderByDesc(Enterprise::getId)
                        .last("LIMIT 1"));
        Enterprise enterprise = existing.isEmpty() ? new Enterprise() : existing.get(0);
        enterprise.setUserId(userId);
        enterprise.setCompanyName(request.getCompanyName().trim());
        enterprise.setLicenseNo(trimToNull(request.getLicenseNo()));
        enterprise.setContactName(trimToNull(request.getContactName()));
        enterprise.setContactPhone(trimToNull(request.getContactPhone()));
        if (enterprise.getId() == null) {
            enterprise.setStatus("pending");
            enterprise.setSubscription("free");
            enterpriseMapper.insert(enterprise);
        } else {
            enterpriseMapper.updateById(enterprise);
        }
        return enterprise;
    }

    public Enterprise getById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!enterprise.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return enterprise;
    }

    public Enterprise getMyEnterprise() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Enterprise> enterprises = enterpriseMapper.selectList(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getUserId, userId)
                        .orderByDesc(Enterprise::getId)
                        .last("LIMIT 1"));
        return enterprises.isEmpty() ? null : enterprises.get(0);
    }

    @Transactional
    public BatchOrderResponse createBatchOrder(BatchOrderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Enterprise enterprise = enterpriseMapper.selectById(request.getEnterpriseId());
        if (enterprise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!userId.equals(enterprise.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if ("rejected".equalsIgnoreCase(enterprise.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "企业资质审核未通过");
        }

        Set<Long> recipientIds = new HashSet<>();
        List<Long> giftRecordIds = new ArrayList<>();
        List<Long> orderIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BatchOrderRequest.EmployeeGift employee : request.getEmployees()) {
            if (!recipientIds.add(employee.getRecipientId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "同一批次不能重复选择收礼人");
            }
            Recipient recipient = recipientMapper.selectById(employee.getRecipientId());
            if (recipient == null) {
                throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
            }
            if (!userId.equals(recipient.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }

            GiftRecord giftRecord = new GiftRecord();
            giftRecord.setUserId(userId);
            giftRecord.setRecipientId(recipient.getId());
            giftRecord.setOccasion(employee.getOccasion() == null
                    || employee.getOccasion().isBlank() ? "other" : employee.getOccasion());
            giftRecord.setBudget(employee.getBudget());
            giftRecord.setStatus(OrderService.STATUS_ORDERED);
            giftRecordMapper.insert(giftRecord);

            Order order = new Order();
            order.setGiftRecordId(giftRecord.getId());
            order.setOrderNo("ENT-" + IdUtil.fastSimpleUUID().substring(0, 20).toUpperCase());
            order.setTotalAmount(employee.getBudget());
            order.setStatus(OrderService.STATUS_ORDERED);
            orderMapper.insert(order);

            giftRecordIds.add(giftRecord.getId());
            orderIds.add(order.getId());
            totalAmount = totalAmount.add(employee.getBudget());
        }

        BatchOrderResponse response = new BatchOrderResponse();
        response.setTotal(orderIds.size());
        response.setTotalAmount(totalAmount);
        response.setGiftRecordIds(giftRecordIds);
        response.setOrderIds(orderIds);
        return response;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
