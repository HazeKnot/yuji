package com.yuji.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.dto.LoginReq;
import com.yuji.auth.dto.LoginResp;
import com.yuji.auth.dto.RegisterReq;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.auth.security.JwtService;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResp register(RegisterReq req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.phone()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? "准妈妈" : req.nickname());
        user.setRole(Roles.MOM);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return toResp(user);
    }

    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.phone()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码不正确");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号不可用，请联系客服");
        }
        return toResp(user);
    }

    private LoginResp toResp(User user) {
        String token = jwtService.issue(user.getId(), user.getRole());
        return new LoginResp(token,
                new LoginResp.UserVO(user.getId(), user.getPhone(), user.getNickname(), user.getRole()));
    }
}
