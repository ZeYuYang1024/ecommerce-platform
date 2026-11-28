package com.ecommerce.member.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberOrderFinanceMapper {
    @Insert("""
            INSERT INTO member_order_finance (order_no) VALUES (#{orderNo})
            ON DUPLICATE KEY UPDATE order_no = member_order_finance.order_no
            """)
    void lockOrder(@Param("orderNo") String orderNo);

    @Select("SELECT fully_refunded FROM member_order_finance WHERE order_no = #{orderNo}")
    boolean isFullyRefunded(@Param("orderNo") String orderNo);

    @Update("UPDATE member_order_finance SET fully_refunded = TRUE WHERE order_no = #{orderNo}")
    void markFullyRefunded(@Param("orderNo") String orderNo);
    @Select("SELECT partial_refund_amount FROM member_order_finance WHERE order_no = #{orderNo}")
    BigDecimal partialRefundAmount(@Param("orderNo") String orderNo);

    @Update("""
            UPDATE member_order_finance SET partial_refund_amount = GREATEST(partial_refund_amount, #{amount})
            WHERE order_no = #{orderNo}
            """)
    void recordPartialRefund(@Param("orderNo") String orderNo, @Param("amount") BigDecimal amount);
}
