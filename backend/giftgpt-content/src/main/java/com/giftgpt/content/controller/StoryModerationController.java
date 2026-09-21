package com.giftgpt.content.controller;
import com.giftgpt.common.result.Result;
import com.giftgpt.content.dto.*;
import com.giftgpt.content.entity.StoryReport;
import com.giftgpt.content.service.StoryModerationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stories")
public class StoryModerationController {
    private final StoryModerationService service;
    @PostMapping("/{id}/reports")
    public Result<Map<String,Object>> report(@PathVariable Long id, @Valid @RequestBody StoryReportRequest request) {
        return Result.ok(service.report(id,request));
    }
    @GetMapping("/moderation/capabilities")
    public Result<Map<String,Boolean>> capabilities() { return Result.ok(Map.of("canModerate",service.canModerate())); }
    @GetMapping("/moderation/reports")
    public Result<Page<StoryReport>> reports(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="pending") String status) {
        return Result.ok(service.reports(page,size,status));
    }
    @GetMapping("/moderation/reports/{id}/story")
    public Result<StoryView> story(@PathVariable Long id) { return Result.ok(service.reportStory(id)); }
    @PostMapping("/moderation/reports/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody StoryReviewRequest request) {
        service.review(id,request); return Result.ok();
    }
}
