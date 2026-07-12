package com.giftgpt.order.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.common.ai.DeepseekClient;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.order.dto.packaging.AiPackagingRequest;
import com.giftgpt.order.dto.packaging.AiPackagingResult;
import com.giftgpt.order.dto.packaging.PackagingTheme;
import com.giftgpt.order.dto.packaging.SavePackagingRequest;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.mapper.PackagingMapper;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackagingService {

    private final PackagingMapper packagingMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final RecipientMapper recipientMapper;

    private final DeepseekClient deepseekClient;

    public List<PackagingTheme> getThemes() {
        return List.of(
            theme("classic", "经典缎面礼盒", "硬质磁吸礼盒，缎面蝴蝶结，丝绒内衬", "29.9"),
            theme("korean", "韩式极简礼盒", "哑光质感，单色丝带，简约标签", "19.9"),
            theme("kraft", "牛皮纸自然风", "牛皮纸+麻绳+干花点缀，环保自然", "9.9"),
            theme("luxury", "轻奢烫金礼盒", "烫金封面，双层蝴蝶结，珠光内衬", "49.9"),
            theme("acrylic", "透明亚克力盒", "透明展示盒，内填拉菲草，丝带装饰", "24.9")
        );
    }

    private PackagingTheme theme(String id, String name, String desc, String price) {
        PackagingTheme t = new PackagingTheme();
        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setPrice(new BigDecimal(price));
        return t;
    }

    public AiPackagingResult aiRecommend(AiPackagingRequest req) {
        if (!deepseekClient.isConfigured()) {
            return fallbackPackaging(req);
        }
        try {
            String content = deepseekClient.chat(
                "你是礼物包装顾问AI助手，总是以JSON格式回复，不添加markdown标记。",
                buildAiPrompt(req), 1024);
            String json = DeepseekClient.stripMarkdown(content);
            ObjectMapper m = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return m.readValue(json, AiPackagingResult.class);
        } catch (Exception e) {
            log.error("AI packaging recommend failed, using fallback", e);
            return fallbackPackaging(req);
        }
    }

    private AiPackagingResult fallbackPackaging(AiPackagingRequest req) {
        // ponytail: 模板兜底，无 key/API 失败时演示不中断
        AiPackagingResult r = new AiPackagingResult();
        r.setPackagingType("classic");
        r.setRibbonColor("金色");
        r.setWrappingStyle("cross");
        return r;
    }

    private String buildAiPrompt(AiPackagingRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位礼物包装顾问。请根据商品信息推荐最合适的包装方案。\n\n");
        sb.append("【商品信息】\n");
        sb.append("- 名称：").append(req.getProductName()).append("\n");
        if (req.getProductCategory() != null) sb.append("- 分类：").append(req.getProductCategory()).append("\n");
        if (req.getProductPrice() != null) sb.append("- 价格：¥").append(req.getProductPrice()).append("\n");
        sb.append("\n【可选礼盒】\n");
        sb.append("1.classic 经典缎面礼盒¥29.9 2.korean 韩式极简¥19.9 3.kraft 牛皮纸¥9.9\n");
        sb.append("4.luxury 轻奢烫金¥49.9 5.acrylic 透明亚克力¥24.9\n\n");
        sb.append("【可选定制】礼带烫金字¥9.9/手写贺卡¥5.0/干花装饰¥12.0/香薰加香¥6.0/定制腰封¥7.0\n");
        sb.append("【丝带绑法】cross经典交叉/side单侧斜绑/double_bow双层蝴蝶结/furoshiki日式风吕敷\n\n");
        sb.append("返回JSON（不要markdown）：\n");
        sb.append("{\"packagingType\":\"classic\",\"ribbonText\":\"祝福\",\"ribbonColor\":\"金色\",");
        sb.append("\"scent\":\"玫瑰\",\"wrappingStyle\":\"cross\"}");
        return sb.toString();
    }

    public Packaging savePackaging(SavePackagingRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        Packaging p = new Packaging();
        p.setUserId(userId);
        p.setTheme(req.getPackagingType());
        p.setCustomText(req.getCustomText());
        p.setPrice(req.getPrice());
        p.setProductName(req.getProductName());
        p.setProductPrice(req.getProductPrice());
        p.setProductImageUrl(req.getProductImageUrl());
        p.setRibbonText(req.getRibbonText());
        p.setRibbonColor(req.getRibbonColor());
        p.setScent(req.getScent());
        p.setPhotoUrl(req.getPhotoUrl());
        p.setWrappingStyle(req.getWrappingStyle());

        if (req.getRecipientId() != null) {
            Recipient recipient = recipientMapper.selectById(req.getRecipientId());
            if (recipient == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            if (!recipient.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            GiftRecord gr = new GiftRecord();
            gr.setUserId(userId);
            gr.setRecipientId(req.getRecipientId());
            gr.setOccasion(req.getOccasion());
            gr.setBudget(req.getProductPrice());
            gr.setStatus("packaged");
            giftRecordMapper.insert(gr);
            p.setGiftRecordId(gr.getId());
        }

        packagingMapper.insert(p);
        return p;
    }

    public Page<Packaging> listPackaging(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<Packaging> p = new Page<>(page, size);
        return packagingMapper.selectPage(p,
                new LambdaQueryWrapper<Packaging>()
                        .eq(Packaging::getUserId, userId)
                        .orderByDesc(Packaging::getCreateTime));
    }
}
