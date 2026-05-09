package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;

import java.util.List;

public interface OrderService {
    OrderVO createOrder(Long userId, CreateOrderRequest request);
    OrderVO getOrder(Long id);
    List<OrderVO> listByUser(Long userId);
    void cancelOrder(Long userId, Long id);
    List<OrderVO> listAll(Integer status);
    void markShipped(Long id);
    void updateStatus(Long id, Integer status);
}
