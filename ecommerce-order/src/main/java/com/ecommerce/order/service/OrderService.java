package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;

import com.ecommerce.order.dto.response.OrderVO;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    OrderVO createOrder(Long userId, CreateOrderRequest request);
    OrderVO getOrder(Long id);
    OrderVO getOrderByOrderNo(Long userId, String orderNo);
    Page<OrderVO> listByUser(Long userId, int page, int size);
    void cancelOrder(Long userId, Long id);
    Page<OrderVO> listAll(int page, int size, Integer status);
    Page<OrderVO> listByMerchant(Long merchantId, int page, int size, Integer status);
    List<String> listOrderNosByMerchant(Long merchantId);
    List<Order> listForRecon(LocalDateTime start, LocalDateTime end);
    void markShipped(Long id, String userType, Long merchantId);
    void updateStatus(Long id, Integer status, String userType, Long merchantId);
}
