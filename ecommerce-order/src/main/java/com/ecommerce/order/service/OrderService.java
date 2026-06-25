package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OutboxMessageVO;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    OrderVO createOrder(Long userId, CreateOrderRequest request);
    OrderVO getOrder(Long userId, Long id);
    OrderVO getOrderByOrderNo(Long userId, String orderNo);
    Page<OrderVO> listByUser(Long userId, int page, int size);
    List<OrderSummaryVO> listSummariesByUser(Long userId, int limit);
    void cancelOrder(Long userId, Long id);
    Page<OrderVO> listAll(int page, int size, Integer status);
    Page<OrderVO> listByMerchant(Long merchantId, int page, int size, Integer status);
    List<String> listOrderNosByMerchant(Long merchantId);
    List<Order> listForRecon(LocalDateTime start, LocalDateTime end);
    void updateStatus(Long id, Integer status, String userType, Long merchantId);
    void applyInventoryCompensation(OrderPaidMessage message);
    Page<OutboxMessageVO> listOutbox(OutboxQuery query, int page, int size);
    OutboxSummary getOutboxSummary(OutboxQuery query);
    int retryOutboxMessage(Long messageId);
    int retryOutboxBatch(OutboxQuery query, int limit);
}
