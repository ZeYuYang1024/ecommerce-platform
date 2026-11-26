package com.ecommerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.order.entity.Order;

public interface OrderMapper extends BaseMapper<Order> {

    default boolean transitionStatus(Long id, int expectedStatus, int targetStatus) {
        return update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getStatus, expectedStatus)
                .set(Order::getStatus, targetStatus)) == 1;
    }
}
