package com.giftgpt.order.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.common.ai.PythonAiClient;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.order.dto.*;
import com.giftgpt.order.entity.Feedback;
import com.giftgpt.order.entity.GreetingCard;
import com.giftgpt.order.entity.LogisticsEvent;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.mapper.FeedbackMapper;
import com.giftgpt.order.mapper.GreetingCardMapper;
import com.giftgpt.order.mapper.LogisticsEventMapper;
import com.giftgpt.order.mapper.OrderMapper;
import com.giftgpt.order.mapper.PackagingMapper;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PackagingMapper packagingMapper;
    private final GreetingCardMapper greetingCardMapper;
    private final FeedbackMapper feedbackMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final RecipientMapper recipientMapper;
    private final LogisticsEventMapper logisticsEventMapper;
    private final PythonAiClient pythonAiClient;

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

        insertMockLogisticsEvents(order.getId());

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

    public LogisticsResponse getLogistics(Long giftRecordId) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getGiftRecordId, giftRecordId));
        if (order == null) throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        LogisticsResponse resp = new LogisticsResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setLogisticsNo(order.getLogisticsNo());
        resp.setLogisticsCompany(order.getLogisticsCompany());
        List<LogisticsEvent> evs = logisticsEventMapper.selectList(
            new LambdaQueryWrapper<LogisticsEvent>()
                .eq(LogisticsEvent::getOrderId, order.getId())
                .orderByAsc(LogisticsEvent::getEventTime));
        resp.setEvents(evs.stream().map(e -> {
            LogisticsResponse.Event ev = new LogisticsResponse.Event();
            ev.setEventTime(e.getEventTime() == null ? "" : e.getEventTime().toString());
            ev.setLocation(e.getLocation());
            ev.setStatus(e.getStatus());
            ev.setDescription(e.getDescription());
            return ev;
        }).collect(Collectors.toList()));
        return resp;
    }

    public GreetingResponse generateGreeting(GreetingGenerateRequest request) {
        GreetingResponse resp = new GreetingResponse();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientName", request.getRecipientName());
            payload.put("relation", request.getRelation() == null ? "" : request.getRelation());
            payload.put("occasion", request.getOccasion() == null ? "" : request.getOccasion());
            payload.put("senderName", request.getSenderName());
            PythonAiClient.GreetingResult r = pythonAiClient.generateGreeting(payload);
            resp.setContent(r.getContent());
            resp.setStyleTemplate(r.getStyleTemplate());
            resp.setAiGenerated(true);
            return resp;
        } catch (Exception e) {
            log.warn("Python greeting unavailable, fallback to template");
            resp.setContent(generateMockGreeting(request));
            resp.setStyleTemplate("classic");
            resp.setAiGenerated(false);
            return resp;
        }
    }

    public Feedback submitFeedback(Long giftRecordId, Feedback feedback) {
        feedback.setGiftRecordId(giftRecordId);
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public List<Feedback> listFeedback(Long giftRecordId) {
        return feedbackMapper.selectList(
            new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getGiftRecordId, giftRecordId)
                .orderByAsc(Feedback::getCreateTime));
    }

    private void insertMockLogisticsEvents(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        List<LogisticsEvent> evs = List.of(
            buildEvent(orderId, now.minusDays(2), "上海仓", "已下单", "订单已创建，等待发货"),
            buildEvent(orderId, now.minusDays(1), "上海转运中心", "已发货", "包裹已从仓库发出"),
            buildEvent(orderId, now, "运输中", "运输中", "包裹运往目的地")
        );
        evs.forEach(logisticsEventMapper::insert);
    }

    private LogisticsEvent buildEvent(Long orderId, LocalDateTime t, String loc, String st, String desc) {
        LogisticsEvent e = new LogisticsEvent();
        e.setOrderId(orderId);
        e.setEventTime(t);
        e.setLocation(loc);
        e.setStatus(st);
        e.setDescription(desc);
        return e;
    }

    private String generateMockGreeting(GreetingGenerateRequest request) {
        return "亲爱的" + request.getRecipientName() + "，\n\n" +
                "在这个特别的日子里，愿这份礼物为你带来温暖与惊喜。" +
                "感谢你一直以来的陪伴，祝你幸福快乐！\n\n" +
                "—— " + request.getSenderName();
    }
}
