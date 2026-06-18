package com.giftgpt.order.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.order.dto.*;
import com.giftgpt.order.entity.Feedback;
import com.giftgpt.order.entity.GreetingCard;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.mapper.FeedbackMapper;
import com.giftgpt.order.mapper.GreetingCardMapper;
import com.giftgpt.order.mapper.OrderMapper;
import com.giftgpt.order.mapper.PackagingMapper;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PackagingMapper packagingMapper;
    private final GreetingCardMapper greetingCardMapper;
    private final FeedbackMapper feedbackMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final RecipientMapper recipientMapper;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        GiftRecord giftRecord = giftRecordMapper.selectById(request.getGiftRecordId());
        if (giftRecord == null || !giftRecord.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
        }

        Order order = new Order();
        order.setGiftRecordId(giftRecord.getId());
        order.setOrderNo(IdUtil.fastSimpleUUID().substring(0, 20));
        order.setTotalAmount(giftRecord.getBudget());
        order.setStatus("pending");
        orderMapper.insert(order);

        if (request.getPackagingThemeId() != null) {
            Packaging packaging = new Packaging();
            packaging.setOrderId(order.getId());
            packaging.setTheme("theme_" + request.getPackagingThemeId());
            packaging.setCustomText(request.getCustomMessage());
            packaging.setPrice(new BigDecimal("19.9"));
            packagingMapper.insert(packaging);
        }

        giftRecord.setStatus("ordered");
        giftRecordMapper.updateById(giftRecord);

        return order;
    }

    public OrderDetailResponse getOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        GiftRecord giftRecord = giftRecordMapper.selectById(order.getGiftRecordId());
        Recipient recipient = recipientMapper.selectById(giftRecord.getRecipientId());
        Packaging packaging = packagingMapper.selectOne(
                new LambdaQueryWrapper<Packaging>().eq(Packaging::getOrderId, order.getId()));
        GreetingCard greeting = null;
        if (giftRecord.getGreetingCardId() != null) {
            greeting = greetingCardMapper.selectById(giftRecord.getGreetingCardId());
        }

        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setLogisticsNo(order.getLogisticsNo());
        resp.setLogisticsCompany(order.getLogisticsCompany());
        resp.setRecipientName(recipient != null ? recipient.getName() : "");
        resp.setPackagingTheme(packaging != null ? packaging.getTheme() : null);
        if (greeting != null) {
            resp.setGreetingContent(greeting.getContent());
            resp.setGreetingVoiceUrl(greeting.getVoiceUrl());
            resp.setGreetingQrCodeUrl(greeting.getQrCodeUrl());
        }
        return resp;
    }

    public Order getLogistics(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    public GreetingCard generateGreeting(GreetingGenerateRequest request) {
        String content = generateMockGreeting(request);
        GreetingCard card = new GreetingCard();
        card.setContent(content);
        card.setStyleTemplate("classic");
        card.setQrCodeUrl("https://placeholder.pics/qrcode/" + IdUtil.fastSimpleUUID().substring(0, 8));
        greetingCardMapper.insert(card);
        return card;
    }

    public Feedback submitFeedback(Long giftRecordId, Feedback feedback) {
        feedback.setGiftRecordId(giftRecordId);
        feedbackMapper.insert(feedback);
        return feedback;
    }

    private String generateMockGreeting(GreetingGenerateRequest request) {
        return "亲爱的" + request.getRecipientName() + "，\n\n" +
                "在这个特别的日子里，愿这份礼物为你带来温暖与惊喜。" +
                "感谢你一直以来的陪伴，祝你幸福快乐！\n\n" +
                "—— " + request.getSenderName();
    }
}
