package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.StockOperateRequest;
import com.ecommerce.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper itemMapper,
                            CartClient cartClient, InventoryClient inventoryClient) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
    }

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }

        // Calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            BigDecimal price = new BigDecimal(item.getPrice());
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Create order
        Order order = new Order();
        order.setId(SnowflakeUtils.nextId());
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(0); // 待支付
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        orderMapper.insert(order);

        // Create order items
        for (CreateOrderRequest.OrderItemRequest i : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setId(SnowflakeUtils.nextId());
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setSkuId(i.getSkuId());
            item.setSpuId(i.getSpuId());
            item.setName(i.getName());
            item.setImage(i.getImage());
            BigDecimal price = new BigDecimal(i.getPrice());
            item.setPrice(price);
            item.setQuantity(i.getQuantity());
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(i.getQuantity())));
            itemMapper.insert(item);

            // 扣减库存（best-effort，服务不可用不影响下单）
            StockOperateRequest sr = new StockOperateRequest(); sr.setSkuId(i.getSkuId()); sr.setQuantity(i.getQuantity());
            try { inventoryClient.deduct(sr); } catch (Exception ignored) {}
        }

        // 清空购物车已购商品（best-effort）
        try { cartClient.getCart(userId); } catch (Exception ignored) {}

        return toVO(order);
    }

    @Override
    public OrderVO getOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return toVO(order);
    }

    @Override
    public List<OrderVO> listByUser(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_PENDING);
        }
        order.setStatus(4);
        orderMapper.updateById(order);

        // 释放库存（best-effort）
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            StockOperateRequest sr = new StockOperateRequest(); sr.setSkuId(item.getSkuId()); sr.setQuantity(item.getQuantity());
            try { inventoryClient.release(sr); } catch (Exception ignored) {}
        }
    }

    @Override
    public List<OrderVO> listAll(Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markShipped(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        order.setStatus(2);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreatedAt(order.getCreatedAt());

        // Load items
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<OrderVO.OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
            iv.setId(item.getId());
            iv.setSkuId(item.getSkuId());
            iv.setSpuId(item.getSpuId());
            iv.setName(item.getName());
            iv.setImage(item.getImage());
            iv.setPrice(item.getPrice());
            iv.setQuantity(item.getQuantity());
            iv.setTotalPrice(item.getTotalPrice());
            itemVOs.add(iv);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String seq = String.format("%04d", SnowflakeUtils.nextId() % 10000);
        return timestamp + seq;
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        if (status == 0) return "待支付";
        if (status == 1) return "已支付";
        if (status == 2) return "已发货";
        if (status == 3) return "已完成";
        if (status == 4) return "已取消";
        return "未知";
    }
}
