package com.ecommerce.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.auth.dto.response.UserVO;
import com.ecommerce.auth.mapper.UserMapper;
import com.ecommerce.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserMapper userMapper;

    public AdminController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/users")
    public Result<List<UserVO>> users() {
        return Result.ok(
            userMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .map(u -> {
                    UserVO vo = new UserVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setPhone(u.getPhone());
                    vo.setAvatar(u.getAvatar());
                    vo.setCreatedAt(u.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList())
        );
    }
}
