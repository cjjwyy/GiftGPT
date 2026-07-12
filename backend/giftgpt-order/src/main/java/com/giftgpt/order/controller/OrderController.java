package com.giftgpt.order.controller;

import com.giftgpt.common.result.Result;
import com.giftgpt.order.dto.*;
import com.giftgpt.order.entity.Feedback;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单与全链路", description = "下单、物流、贺卡、反馈")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单")
    @PostMapping("/gifts/{id}/order")
    public Result<Order> createOrder(@PathVariable Long id, @Valid @RequestBody CreateOrderRequest request) {
        request.setGiftRecordId(id);
        return Result.ok(orderService.createOrder(request));
    }

    @Operation(summary = "物流追踪")
    @GetMapping("/gifts/{id}/logistics")
    public Result<LogisticsResponse> logistics(@PathVariable Long id) {
        return Result.ok(orderService.getLogistics(id));
    }

    @Operation(summary = "AI 生成贺卡文案")
    @PostMapping("/greetings/generate")
    public Result<GreetingResponse> generateGreeting(@RequestBody GreetingGenerateRequest request) {
        return Result.ok(orderService.generateGreeting(request));
    }

    @Operation(summary = "收礼人反馈")
    @PostMapping("/gifts/{id}/feedback")
    public Result<Feedback> feedback(@PathVariable Long id, @RequestBody Feedback feedback) {
        return Result.ok(orderService.submitFeedback(id, feedback));
    }

    @Operation(summary = "反馈列表")
    @GetMapping("/gifts/{id}/feedback")
    public Result<List<Feedback>> listFeedback(@PathVariable Long id) {
        return Result.ok(orderService.listFeedback(id));
    }
}
