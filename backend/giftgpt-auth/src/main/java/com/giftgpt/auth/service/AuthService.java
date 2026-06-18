package com.giftgpt.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.giftgpt.auth.dto.LoginRequest;
import com.giftgpt.auth.dto.LoginResponse;
import com.giftgpt.auth.dto.RegisterRequest;
import com.giftgpt.auth.entity.User;
import com.giftgpt.auth.mapper.UserMapper;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        return resp;
    }

    public LoginResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(BCrypt.hashpw(request.getPassword()));
        String defaultNickname = "用户" +
                (request.getPhone().length() >= 8 ? request.getPhone().substring(request.getPhone().length() - 4) : request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : defaultNickname);
        user.setAuthProvider("local");
        user.setStatus(1);
        userMapper.insert(user);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        return resp;
    }

    public void logout() {
        StpUtil.logout();
    }
}
