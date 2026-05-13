package com.ecommerce.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.auth.client.MerchantStatsClient;
import com.ecommerce.auth.client.ProductStatsClient;
import com.ecommerce.common.dto.CreateMerchantAccountRequest;
import com.ecommerce.auth.dto.response.DashboardStatsVO;
import com.ecommerce.common.dto.MerchantAccountVO;
import com.ecommerce.auth.dto.response.UserVO;
import com.ecommerce.auth.entity.AdminUser;
import com.ecommerce.auth.mapper.AdminUserMapper;
import com.ecommerce.auth.mapper.UserMapper;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import jakarta.validation.Valid;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final MerchantStatsClient merchantStatsClient;
    private final ProductStatsClient productStatsClient;

    public AdminController(UserMapper userMapper,
                           AdminUserMapper adminUserMapper,
                           MerchantStatsClient merchantStatsClient,
                           ProductStatsClient productStatsClient) {
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
        this.merchantStatsClient = merchantStatsClient;
        this.productStatsClient = productStatsClient;
    }

    @GetMapping("/users")
    public Result<Page<UserVO>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<com.ecommerce.auth.entity.User> pageReq = new Page<>(page, size);
        userMapper.selectPage(pageReq, new LambdaQueryWrapper<>());
        return Result.ok(new Page<UserVO>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal()).setRecords(
            pageReq.getRecords().stream()
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
        ));
    }

    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsVO> dashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<>()));

        try {
            var res = merchantStatsClient.stats();
            if (res.getData() != null) {
                stats.setMerchantCount(res.getData().getMerchantCount());
                stats.setPendingAuditCount(res.getData().getPendingAuditCount());
            }
        } catch (Exception ignored) {}

        try {
            var res = productStatsClient.stats();
            if (res.getData() != null) {
                stats.setProductCount(res.getData().getProductCount());
            }
        } catch (Exception ignored) {}

        return Result.ok(stats);
    }

    @PostMapping("/merchant-account")
    public Result<MerchantAccountVO> createMerchantAccount(@Valid @RequestBody CreateMerchantAccountRequest request) {
        Long merchantId = request.getMerchantId();
        String username = "m_" + merchantId;
        String password = DigestUtils.md5DigestAsHex(("merchant_" + merchantId).getBytes(StandardCharsets.UTF_8));

        AdminUser existing = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getMerchantId, merchantId));
        if (existing != null) {
            MerchantAccountVO vo = new MerchantAccountVO();
            vo.setUsername(existing.getUsername());
            vo.setCreated(false);
            return Result.ok(vo);
        }

        AdminUser admin = new AdminUser();
        admin.setId(SnowflakeUtils.nextId());
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setType("merchant");
        admin.setMerchantId(merchantId);
        admin.setStatus(1);
        adminUserMapper.insert(admin);

        MerchantAccountVO vo = new MerchantAccountVO();
        vo.setUsername(username);
        vo.setCreated(true);
        return Result.ok(vo);
    }
}
