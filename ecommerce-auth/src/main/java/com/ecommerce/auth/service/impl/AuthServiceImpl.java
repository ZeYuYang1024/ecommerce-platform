package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.common.AuthErrorCode;
import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.dto.response.UserVO;
import com.ecommerce.auth.entity.AdminUser;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.mapper.AdminUserMapper;
import com.ecommerce.auth.mapper.UserMapper;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.common.util.SnowflakeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;

    public AuthServiceImpl(UserMapper userMapper, AdminUserMapper adminUserMapper) {
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists) {
            throw new BusinessException(AuthErrorCode.USERNAME_DUPLICATE);
        }

        User user = new User();
        user.setId(SnowflakeUtils.nextId());
        user.setUsername(request.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setPhone(request.getPhone());
        userMapper.insert(user);

        String token = JwtUtils.generate(user.getId(), user.getUsername(), "user", "user");
        return LoginResponse.of(token, user.getId(), user.getUsername(), "user");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        String encrypted = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!encrypted.equals(user.getPassword())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        String token = JwtUtils.generate(user.getId(), user.getUsername(), "user", "user");
        return LoginResponse.of(token, user.getId(), user.getUsername(), "user");
    }

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, request.getUsername()));
        if (admin == null) {
            throw new BusinessException(AuthErrorCode.ADMIN_NOT_FOUND);
        }
        String encrypted = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!encrypted.equals(admin.getPassword())) {
            throw new BusinessException(AuthErrorCode.ADMIN_PASSWORD_ERROR);
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(AuthErrorCode.ADMIN_FORBIDDEN);
        }

        String adminType = admin.getType() != null ? admin.getType() : "super_admin";
        String token = JwtUtils.generate(admin.getId(), admin.getUsername(), "admin", adminType, admin.getMerchantId());
        return LoginResponse.of(token, admin.getId(), admin.getUsername(), adminType, admin.getMerchantId());
    }

    @Override
    public Long validateToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            return JwtUtils.getUserId(token);
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    @Override
    public UserVO updateProfile(Long userId, String phone, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (phone != null) user.setPhone(phone);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
        return getProfile(userId);
    }
}
