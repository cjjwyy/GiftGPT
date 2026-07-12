package com.giftgpt.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.user.dto.RecipientCreateRequest;
import com.giftgpt.user.dto.RecipientDetailResponse;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientProfile;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientProfileMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientMapper recipientMapper;
    private final RecipientTagMapper tagMapper;
    private final RecipientProfileMapper profileMapper;

    @Transactional
    public Recipient create(RecipientCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = new Recipient();
        recipient.setUserId(userId);
        recipient.setName(request.getName());
        recipient.setRelation(request.getRelation());
        recipient.setGender(request.getGender());
        recipient.setAgeRange(request.getAgeRange());
        recipient.setMbti(request.getMbti());
        recipient.setPersonality(request.getPersonality());
        recipient.setRecentPurchases(request.getRecentPurchases());
        recipient.setNote(request.getNote());
        recipientMapper.insert(recipient);

        if (request.getTags() != null) {
            for (String tagCode : request.getTags()) {
                RecipientTag tag = new RecipientTag();
                tag.setRecipientId(recipient.getId());
                tag.setTagCode(tagCode);
                tag.setTagName(tagCode);
                tagMapper.insert(tag);
            }
        }
        return recipient;
    }

    @Transactional
    public Recipient update(Long id, RecipientCreateRequest request) {
        Recipient recipient = getOwnRecipient(id);
        recipient.setName(request.getName());
        recipient.setRelation(request.getRelation());
        recipient.setGender(request.getGender());
        recipient.setAgeRange(request.getAgeRange());
        recipient.setMbti(request.getMbti());
        recipient.setPersonality(request.getPersonality());
        recipient.setRecentPurchases(request.getRecentPurchases());
        recipient.setNote(request.getNote());
        recipientMapper.updateById(recipient);

        tagMapper.delete(new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, id));
        if (request.getTags() != null) {
            for (String tagCode : request.getTags()) {
                RecipientTag tag = new RecipientTag();
                tag.setRecipientId(id);
                tag.setTagCode(tagCode);
                tag.setTagName(tagCode);
                tagMapper.insert(tag);
            }
        }
        return recipient;
    }

    @Transactional
    public void delete(Long id) {
        Recipient recipient = getOwnRecipient(id);
        tagMapper.delete(new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, id));
        profileMapper.delete(new LambdaQueryWrapper<RecipientProfile>().eq(RecipientProfile::getRecipientId, id));
        recipientMapper.deleteById(recipient.getId());
    }

    public Page<Recipient> list(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<Recipient> p = new Page<>(page, size);
        return recipientMapper.selectPage(p,
                new LambdaQueryWrapper<Recipient>().eq(Recipient::getUserId, userId).orderByDesc(Recipient::getCreateTime));
    }

    public RecipientDetailResponse getDetail(Long id) {
        Recipient recipient = getOwnRecipient(id);
        List<RecipientTag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, id));
        RecipientProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<RecipientProfile>().eq(RecipientProfile::getRecipientId, id));

        RecipientDetailResponse resp = new RecipientDetailResponse();
        resp.setId(recipient.getId());
        resp.setName(recipient.getName());
        resp.setRelation(recipient.getRelation());
        resp.setGender(recipient.getGender());
        resp.setAgeRange(recipient.getAgeRange());
        resp.setMbti(recipient.getMbti());
        resp.setPersonality(recipient.getPersonality());
        resp.setRecentPurchases(recipient.getRecentPurchases());
        resp.setNote(recipient.getNote());
        resp.setTags(tags.stream().map(RecipientTag::getTagCode).collect(Collectors.toList()));
        if (profile != null) {
            resp.setPersonalityDesc(profile.getPersonalityDesc());
            resp.setHobbyList(profile.getHobbyList());
            resp.setSocialAnalysis(profile.getSocialAnalysis());
        }
        return resp;
    }

    private Recipient getOwnRecipient(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = recipientMapper.selectById(id);
        if (recipient == null) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }
        if (!recipient.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return recipient;
    }
}
