package com.giftgpt.recommendation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.recommendation.dto.RecommendFeedbackRequest;
import com.giftgpt.recommendation.dto.RecommendRequest;
import com.giftgpt.recommendation.dto.RecommendResponse;
import com.giftgpt.recommendation.entity.RecommendationHistory;
import com.giftgpt.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 礼物推荐", description = "条件检索推荐、历史记录、反馈")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "条件检索推荐")
    @PostMapping("/search")
    public Result<RecommendResponse> search(@Valid @RequestBody RecommendRequest request) {
        return Result.ok(recommendationService.search(request));
    }

    @Operation(summary = "历史推荐记录")
    @GetMapping("/history")
    public Result<Page<RecommendationHistory>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(recommendationService.history(page, size));
    }

    @Operation(summary = "推荐反馈")
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody RecommendFeedbackRequest request) {
        recommendationService.feedback(id, request);
        return Result.ok();
    }
}
