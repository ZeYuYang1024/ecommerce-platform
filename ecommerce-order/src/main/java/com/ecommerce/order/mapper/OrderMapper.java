package com.ecommerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE `order` SET inventory_reserved = TRUE WHERE order_no = #{orderNo} AND status IN (0, 1) AND deleted = 0")
    void markInventoryReserved(@Param("orderNo") String orderNo);

    @Update("UPDATE `order` SET payment_refund_required = TRUE WHERE id = #{id} AND deleted = 0")
    void markCompensationRefundRequired(@Param("id") Long id);

    default boolean transitionStatus(Long id, int expectedStatus, int targetStatus) {
        return update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getStatus, expectedStatus)
                .set(Order::getStatus, targetStatus)) == 1;
    }
}
