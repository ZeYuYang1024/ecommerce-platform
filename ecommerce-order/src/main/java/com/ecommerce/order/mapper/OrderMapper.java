package com.ecommerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE `order` SET inventory_reserved = TRUE WHERE order_no = #{orderNo} AND status IN (0, 1) AND deleted = 0")
    void markInventoryReserved(@Param("orderNo") String orderNo);

    @Update("UPDATE `order` SET payment_refund_required = TRUE WHERE id = #{id} AND deleted = 0")
    void markCompensationRefundRequired(@Param("id") Long id);

    @Update("UPDATE `order` SET points_reservation_no = #{reservationNo} WHERE id = #{id}")
    void setPointsReservation(@Param("id") Long id, @Param("reservationNo") String reservationNo);

    @Update("UPDATE `order` SET manual_refund_key = #{key} WHERE id = #{id} AND deleted = 0")
    void setManualRefundKey(@Param("id") Long id, @Param("key") String key);

    @Update("UPDATE `order` SET refunded_amount = GREATEST(refunded_amount, #{amount}) WHERE id = #{id} AND deleted = 0")
    void recordRefundedAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Select("""
            SELECT id FROM `order`
            WHERE deleted = 0 AND status = 0 AND created_at <= #{cutoff}
            ORDER BY created_at, id LIMIT #{limit}
            """)
    List<Long> findExpiredPending(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    default boolean transitionStatus(Long id, int expectedStatus, int targetStatus) {
        return update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getStatus, expectedStatus)
                .set(Order::getStatus, targetStatus)) == 1;
    }
}
