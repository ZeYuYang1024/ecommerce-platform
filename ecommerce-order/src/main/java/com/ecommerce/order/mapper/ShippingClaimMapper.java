package com.ecommerce.order.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ShippingClaimMapper {
    record Claim(Long orderId, String shippingNo, Long shippingId, boolean managed) {}

    @Select("""
            SELECT EXISTS (SELECT 1 FROM order_shipping_claim WHERE order_id = #{orderId}
                AND (expires_at IS NULL OR expires_at > NOW()))
            """)
    boolean existsForOrder(@Param("orderId") Long orderId);

    @Select("""
            SELECT EXISTS (SELECT 1 FROM order_shipping_claim WHERE order_id = #{orderId}
                AND shipping_no != #{shippingNo} AND (expires_at IS NULL OR expires_at > NOW()))
            """)
    boolean existsOtherForOrder(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo);

    @Select("""
            SELECT order_id AS orderId, shipping_no AS shippingNo, shipping_id AS shippingId, managed
            FROM order_shipping_claim WHERE order_id = #{orderId} AND expires_at <= NOW()
            ORDER BY expires_at, shipping_no
            """)
    List<Claim> findExpiredForOrder(@Param("orderId") Long orderId);

    @Select("""
            SELECT order_id AS orderId, shipping_no AS shippingNo, shipping_id AS shippingId, managed
            FROM order_shipping_claim WHERE expires_at <= NOW()
            ORDER BY expires_at, order_id, shipping_no LIMIT 100
            """)
    List<Claim> findExpired();

    @Insert("""
            INSERT INTO order_shipping_claim (order_id, shipping_no, shipping_id, managed, expires_at)
            VALUES (#{orderId}, #{shippingNo}, #{shippingId}, #{managed}, NOW() + INTERVAL 30 MINUTE)
            ON DUPLICATE KEY UPDATE expires_at =
                IF(order_shipping_claim.expires_at IS NULL, NULL, NOW() + INTERVAL 30 MINUTE)
            """)
    int claim(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo,
              @Param("shippingId") Long shippingId, @Param("managed") boolean managed);

    @Update("""
            UPDATE order_shipping_claim SET expires_at = NULL
            WHERE order_id = #{orderId} AND shipping_no = #{shippingNo}
            """)
    int confirm(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo);

    @Update("""
            UPDATE order_shipping_claim SET expires_at = NULL
            WHERE order_id = #{orderId} AND shipping_no = #{shippingNo} AND expires_at <= NOW()
            """)
    int confirmExpired(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo);

    @Delete("""
            DELETE FROM order_shipping_claim WHERE order_id = #{orderId} AND shipping_no = #{shippingNo}
                AND expires_at <= NOW()
            """)
    int releaseExpired(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo);

    @Delete("""
            DELETE FROM order_shipping_claim WHERE order_id = #{orderId} AND shipping_no = #{shippingNo}
                AND expires_at IS NOT NULL
            """)
    int release(@Param("orderId") Long orderId, @Param("shippingNo") String shippingNo);
}
