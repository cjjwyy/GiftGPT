package com.giftgpt.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.content.dto.*;
import com.giftgpt.content.entity.*;
import com.giftgpt.content.mapper.*;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StoryModerationService {
    private final StoryMapper storyMapper;
    private final StoryReportMapper reportMapper;
    @Value("${giftgpt.community.moderator-ids:}")
    private String moderatorIds = "";

    public boolean canModerate() {
        if (!StpUtil.isLogin()) return false;
        String id = Long.toString(StpUtil.getLoginIdAsLong());
        return Arrays.stream(moderatorIds.split(",")).map(String::trim).anyMatch(id::equals);
    }
    private void requireModerator() {
        StpUtil.checkLogin();
        if (!canModerate()) throw new BusinessException(ResultCode.FORBIDDEN);
    }

    @Transactional
    public Map<String,Object> report(Long storyId, StoryReportRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Story story = storyMapper.lockById(storyId);
        if (story == null || !Integer.valueOf(1).equals(story.getStatus())) throw new BusinessException(ResultCode.NOT_FOUND);
        StoryReport existing = reportMapper.selectOne(new LambdaQueryWrapper<StoryReport>()
                .eq(StoryReport::getStoryId, storyId).eq(StoryReport::getReporterId, userId));
        if (existing != null) return Map.of("id", existing.getId(), "status", existing.getStatus());
        StoryReport report = new StoryReport();
        report.setStoryId(storyId); report.setReporterId(userId); report.setReason(request.getReason());
        report.setDetail(request.getDetail().trim()); report.setStatus("pending");
        reportMapper.insert(report);
        return Map.of("id", report.getId(), "status", report.getStatus());
    }

    public Page<StoryReport> reports(int page, int size, String status) {
        requireModerator();
        if (!List.of("pending", "hide", "dismiss").contains(status)) throw new BusinessException(ResultCode.BAD_REQUEST);
        return reportMapper.selectPage(new Page<>(Math.max(1,page), Math.max(1,Math.min(100,size))),
                new LambdaQueryWrapper<StoryReport>().eq(StoryReport::getStatus,status)
                        .orderByAsc(StoryReport::getId));
    }

    public StoryView reportStory(Long id) {
        requireModerator();
        StoryReport report = reportMapper.selectById(id);
        if (report == null) throw new BusinessException(ResultCode.NOT_FOUND);
        Story story = storyMapper.selectById(report.getStoryId());
        if (story == null) throw new BusinessException(ResultCode.NOT_FOUND);
        // Even moderators see anonymized display data; report authors are in the audit queue only.
        return StoryView.from(story, null);
    }

    @Transactional
    public void review(Long id, StoryReviewRequest request) {
        requireModerator();
        if (!List.of("hide", "dismiss").contains(request.getAction())) throw new BusinessException(ResultCode.BAD_REQUEST);
        StoryReport report = reportMapper.lockById(id);
        if (report == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!"pending".equals(report.getStatus())) throw new BusinessException(ResultCode.CONFLICT.getCode(), "举报已处理，请刷新");
        if ("hide".equals(request.getAction())) {
            Story story = storyMapper.lockById(report.getStoryId());
            if (story == null) throw new BusinessException(ResultCode.NOT_FOUND);
            story.setStatus(0); storyMapper.updateById(story);
        }
        report.setStatus(request.getAction()); report.setModeratorId(StpUtil.getLoginIdAsLong());
        report.setDecisionNote(request.getNote().trim()); reportMapper.updateById(report);
    }
}
