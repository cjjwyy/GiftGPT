package com.giftgpt.order.controller;

import com.giftgpt.common.result.Result;
import com.giftgpt.order.dto.*;
import com.giftgpt.order.entity.Feedback;
import com.giftgpt.order.entity.GreetingCard;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public Result<GreetingResponse> generateGreeting(@Valid @RequestBody GreetingGenerateRequest request) {
        return Result.ok(orderService.generateGreeting(request));
    }

    @Operation(summary = "收礼人反馈")
    @PostMapping("/gifts/{id}/feedback")
    public Result<Feedback> feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest feedback) {
        return Result.ok(orderService.submitFeedback(id, feedback));
    }

    @Operation(summary = "反馈列表")
    @GetMapping("/gifts/{id}/feedback")
    public Result<List<Feedback>> listFeedback(@PathVariable Long id) {
        return Result.ok(orderService.listFeedback(id));
    }

    @Operation(summary = "生成收礼方一次性反馈链接")
    @PostMapping("/gifts/{id}/feedback-link")
    public Result<FeedbackLinkResponse> feedbackLink(@PathVariable Long id) {
        return Result.ok(orderService.createFeedbackLink(id));
    }

    @Operation(summary = "查看收礼方反馈页面信息")
    @GetMapping("/public/feedback/{token}")
    public Result<PublicFeedbackGiftResponse> publicFeedbackGift(@PathVariable String token) {
        return Result.ok(orderService.getPublicFeedbackGift(token));
    }

    @Operation(summary = "提交收礼方一次性反馈")
    @PostMapping("/public/feedback/{token}")
    public Result<Feedback> publicFeedback(
            @PathVariable String token, @Valid @RequestBody FeedbackRequest request) {
        return Result.ok(orderService.submitPublicFeedback(token, request));
    }

    @Operation(summary = "查看贺卡")
    @GetMapping("/gifts/{id}/greeting")
    public Result<GreetingCard> greeting(@PathVariable Long id) {
        return Result.ok(orderService.getGreeting(id));
    }

    @Operation(summary = "上传语音贺卡")
    @PostMapping(value = "/gifts/{id}/greeting/voice", consumes = "multipart/form-data")
    public Result<GreetingCard> uploadGreetingVoice(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return Result.ok(orderService.uploadGreetingVoice(id, file));
    }
}
