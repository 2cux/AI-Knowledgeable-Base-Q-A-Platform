package com.example.aikb.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.entity.User;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.UserMapper;
import com.example.aikb.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        int rows = userMapper.insert(user);
        if (rows != 1 || user.getId() == null) {
            throw new BusinessException("用户注册失败");
        }
        return user.getId();
    }
}
