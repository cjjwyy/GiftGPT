package com.giftgpt.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.recommendation.dto.RecommendFeedbackRequest;
import com.giftgpt.recommendation.dto.RecommendItem;
import com.giftgpt.recommendation.dto.RecommendRequest;
import com.giftgpt.recommendation.dto.RecommendResponse;
import com.giftgpt.recommendation.entity.RecommendationHistory;
import com.giftgpt.recommendation.mapper.RecommendationHistoryMapper;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecipientMapper recipientMapper;
    private final RecipientTagMapper recipientTagMapper;
    private final RecommendationHistoryMapper historyMapper;

    public RecommendResponse search(RecommendRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = recipientMapper.selectById(request.getRecipientId());
        if (recipient == null || !recipient.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }

        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipient.getId()));
        List<String> tagNames = tags.stream().map(RecipientTag::getTagName).collect(Collectors.toList());

        RecommendResponse response = new RecommendResponse();
        response.setRecipientId(recipient.getId());
        response.setRecipientName(recipient.getName());
        response.setOccasion(request.getOccasion());
        response.setBudget(request.getBudget());
        response.setItems(generateMockItems(request, tagNames));
        response.setSummary("根据" + recipient.getName() + "的" + String.join("、", tagNames) +
                "等特征，在" + request.getOccasion() + "场景下为您推荐以下礼物");

        RecommendationHistory history = new RecommendationHistory();
        history.setUserId(userId);
        history.setRecipientId(recipient.getId());
        history.setScene(request.getOccasion());
        history.setBudget(request.getBudget());
        history.setResult(JSONUtil.toJsonStr(response));
        historyMapper.insert(history);

        return response;
    }

    public Page<RecommendationHistory> history(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<RecommendationHistory> p = new Page<>(page, size);
        return historyMapper.selectPage(p,
                new LambdaQueryWrapper<RecommendationHistory>()
                        .eq(RecommendationHistory::getUserId, userId)
                        .orderByDesc(RecommendationHistory::getCreateTime));
    }

    public void feedback(Long id, RecommendFeedbackRequest request) {
        RecommendationHistory history = historyMapper.selectById(id);
        if (history != null) {
            history.setFeedback(request.getFeedback());
            historyMapper.updateById(history);
        }
    }

    private List<RecommendItem> generateMockItems(RecommendRequest request, List<String> tags) {
        List<RecommendItem> items = new ArrayList<>();

        if (tags.contains("文艺") || tags.contains("文学")) {
            items.add(buildItem(1L, "手写羊皮卷情书定制礼盒", new BigDecimal("129.00"),
                    "https://placeholder.pics/gift/1", "结合TA的文艺气质，手写体+复古羊皮纸营造仪式感"));
        }
        if (tags.contains("摄影") || tags.contains("户外")) {
            items.add(buildItem(2L, "富士拍立得instax mini 12", new BigDecimal("459.00"),
                    "https://placeholder.pics/gift/2", "即时记录旅行瞬间，与TA的摄影+户外属性完美契合"));
        }
        if (tags.contains("极客") || tags.contains("科技")) {
            items.add(buildItem(3L, "机械键盘定制键帽套装", new BigDecimal("299.00"),
                    "https://placeholder.pics/gift/3", "极客属性标配，可自定义配色方案"));
        }
        if (tags.contains("养生") || tags.contains("健康")) {
            items.add(buildItem(4L, "智能温控泡脚桶 + 草本足浴包礼盒", new BigDecimal("199.00"),
                    "https://placeholder.pics/gift/4", "养生派首选，智能恒温+多档按摩"));
        }
        if (tags.contains("音乐") || tags.contains("艺术")) {
            items.add(buildItem(5L, "黑胶唱片装饰灯 + 定制歌单二维码", new BigDecimal("168.00"),
                    "https://placeholder.pics/gift/5", "音乐美学二合一，可扫码听你为TA精选的歌单"));
        }

        // Generic items
        items.add(buildItem(10L, "永生花音乐盒礼盒", new BigDecimal("239.00"),
                "https://placeholder.pics/gift/10", "经典浪漫之选，永不凋谢的心意"));
        items.add(buildItem(11L, "定制名字925银项链", new BigDecimal("189.00"),
                "https://placeholder.pics/gift/11", "个性化定制，刻上TA的名字"));
        items.add(buildItem(12L, "北欧极简香薰蜡烛礼盒", new BigDecimal("89.00"),
                "https://placeholder.pics/gift/12", "营造温馨氛围，品质生活之选"));

        return items.stream()
                .filter(item -> item.getPrice().compareTo(request.getBudget()) <= 0)
                .peek(item -> item.setScore(0.85 + Math.random() * 0.15))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(8)
                .collect(Collectors.toList());
    }

    private RecommendItem buildItem(Long id, String name, BigDecimal price, String imageUrl, String reason) {
        RecommendItem item = new RecommendItem();
        item.setProductId(id);
        item.setProductName(name);
        item.setPrice(price);
        item.setImageUrl(imageUrl);
        item.setPlatform("京东");
        item.setPlatformUrl("https://www.jd.com");
        item.setReason(reason);
        item.setScore(0.90);
        return item;
    }
}
