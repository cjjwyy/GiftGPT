package com.giftgpt.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.auth.entity.DataAuthorization;
import com.giftgpt.auth.mapper.DataAuthorizationMapper;
import com.giftgpt.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "数据授权", description = "隐私授权面板 — 细粒度数据授权/撤回")
@RestController
@RequestMapping("/api/v1/auth/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final DataAuthorizationMapper authorizationMapper;

    @Operation(summary = "获取数据授权列表")
    @GetMapping
    public Result<List<DataAuthorization>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<DataAuthorization> list = authorizationMapper.selectList(
                new LambdaQueryWrapper<DataAuthorization>().eq(DataAuthorization::getUserId, userId));
        return Result.ok(list);
    }

    @Operation(summary = "修改授权范围")
    @PutMapping("/{id}")
    public Result<DataAuthorization> update(@PathVariable Long id, @RequestBody DataAuthorization body) {
        DataAuthorization auth = authorizationMapper.selectById(id);
        if (auth == null || !auth.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.fail(403, "无权操作");
        }
        auth.setAuthorizedScope(body.getAuthorizedScope());
        auth.setExpireAt(body.getExpireAt());
        authorizationMapper.updateById(auth);
        return Result.ok(auth);
    }

    @Operation(summary = "撤回授权")
    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        DataAuthorization auth = authorizationMapper.selectById(id);
        if (auth == null || !auth.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            return Result.fail(403, "无权操作");
        }
        auth.setStatus("revoked");
        authorizationMapper.updateById(auth);
        return Result.ok();
    }
}
