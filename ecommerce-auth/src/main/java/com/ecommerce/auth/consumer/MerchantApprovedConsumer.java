package com.ecommerce.auth.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.auth.entity.AdminUser;
import com.ecommerce.auth.mapper.AdminUserMapper;
import com.ecommerce.common.dto.MerchantApprovedMessage;
import com.ecommerce.common.util.SnowflakeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "merchant-approved",
    consumerGroup = "${spring.application.name}-consumer"
)
public class MerchantApprovedConsumer implements RocketMQListener<MerchantApprovedMessage> {

    private final AdminUserMapper adminUserMapper;

    public MerchantApprovedConsumer(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public void onMessage(MerchantApprovedMessage msg) {
        log.info("MQ merchant-approved: id={}", msg.getMerchantId());
        // 检查是否已存在
        AdminUser existing = adminUserMapper.selectOne(
            new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getMerchantId, msg.getMerchantId()));
        if (existing != null) {
            log.info("Merchant admin already exists: {}", existing.getUsername());
            return;
        }
        String username = "m_" + msg.getMerchantId();
        String password = DigestUtils.md5DigestAsHex(("merchant_" + msg.getMerchantId()).getBytes(StandardCharsets.UTF_8));
        AdminUser admin = new AdminUser();
        admin.setId(SnowflakeUtils.nextId());
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setType("merchant");
        admin.setMerchantId(msg.getMerchantId());
        admin.setStatus(1);
        adminUserMapper.insert(admin);
        log.info("Merchant admin created: {}", username);
    }
}
