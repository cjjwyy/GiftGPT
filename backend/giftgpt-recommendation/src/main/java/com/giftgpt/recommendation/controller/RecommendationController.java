package com.giftgpt.recommendation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.recommendation.dto.AiGiftsResponse;
import com.giftgpt.recommendation.dto.MatchRequest;
import com.giftgpt.recommendation.dto.PersonalitySnapshot;
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

import java.util.List;
import java.util.Map;

@Tag(name = "AI 礼物推荐", description = "条件检索推荐、历史记录、反馈")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "条件检索推荐（一次性，等价于三步合体）")
    @PostMapping("/search")
    public Result<RecommendResponse> search(@Valid @RequestBody RecommendRequest request) {
        return Result.ok(recommendationService.search(request));
    }

    @Operation(summary = "第一步：分析收礼人性格")
    @PostMapping("/analyze")
    public Result<PersonalitySnapshot> analyze(@RequestBody Map<String, Long> body) {
        return Result.ok(recommendationService.analyze(body.get("recipientId")));
    }

    @Operation(summary = "第二步：AI 智能判断合适礼物")
    @PostMapping("/ai-gifts")
    public Result<AiGiftsResponse> aiGifts(@Valid @RequestBody RecommendRequest request) {
        return Result.ok(recommendationService.generateAiGifts(request));
    }

    @Operation(summary = "第三步：在各大购物平台搜索并生成最终推荐")
    @PostMapping("/match")
    public Result<RecommendResponse> match(@RequestBody MatchRequest request) {
        return Result.ok(recommendationService.matchAndSearch(request));
    }

    @Operation(summary = "历史推荐记录")
    @GetMapping("/history")
    public Result<Page<RecommendationHistory>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(recommendationService.history(page, size));
    }

    @Operation(summary = "历史推荐详情")
    @GetMapping("/history/{id}")
    public Result<RecommendResponse> historyDetail(@PathVariable Long id) {
        return Result.ok(recommendationService.getHistoryDetail(id));
    }

    @Operation(summary = "推荐反馈")
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody RecommendFeedbackRequest request) {
        recommendationService.feedback(id, request);
        return Result.ok();
    }

    @Operation(summary = "批量删除历史记录")
    @DeleteMapping("/history")
    public Result<Void> deleteHistory(@RequestBody Map<String, List<Long>> body) {
        recommendationService.deleteHistories(body.get("ids"));
        return Result.ok();
    }
}
