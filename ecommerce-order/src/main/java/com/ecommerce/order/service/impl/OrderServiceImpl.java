package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.order.client.ProductSpuClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import com.ecommerce.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final CartClient cartClient;
    private final ProductSpuClient productSpuClient;
    private final RocketMQTemplate rocketMQTemplate;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper itemMapper, ProductSpuClient productSpuClient,
                            CartClient cartClient, RocketMQTemplate rocketMQTemplate) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.cartClient = cartClient;
        this.productSpuClient = productSpuClient;
        this.rocketMQTemplate = rocketMQTemplate;
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

            // 发送MQ异步扣减库存
            OrderItemMessage oim = new OrderItemMessage(i.getSkuId(), i.getQuantity());
            try { rocketMQTemplate.syncSend("order-created",
                new OrderInventoryMessage(order.getOrderNo(), java.util.Collections.singletonList(oim))); } catch (Exception e) { log.error("MQ deduct failed", e); }
        }

        // 清空购物车已购商品（best-effort）
        try { cartClient.getCart(userId); } catch (Exception ignored) {}

        return toVO(order, java.util.Collections.emptyMap());
    }

    @Override
    public OrderVO getOrderByOrderNo(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        return getOrder(order.getId());
    }

    @Override
    public OrderVO getOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(Collections.singletonList(order));
        return toVO(order, itemsMap);
    }

    @Override
    public Page<OrderVO> listByUser(Long userId, int page, int size) {
        Page<Order> pageReq = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageReq,
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> toVO(o, itemsMap)).collect(Collectors.toList());
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<OrderSummaryVO> listSummariesByUser(Long userId, int limit) {
        int size = normalizeSummaryLimit(limit);
        Page<Order> result = orderMapper.selectPage(new Page<>(1, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        if (result.getRecords().isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        return result.getRecords().stream()
                .map(order -> toSummaryVO(order, itemsMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
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

        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<OrderItemMessage> releaseItems = items.stream()
            .map(i -> new OrderItemMessage(i.getSkuId(), i.getQuantity()))
            .collect(Collectors.toList());
        try { rocketMQTemplate.syncSend("order-cancelled",
            new OrderInventoryMessage(order.getOrderNo(), releaseItems)); } catch (Exception e) { log.error("MQ release failed", e); }
    }

    @Override
    public Page<OrderVO> listByMerchant(Long merchantId, int page, int size, Integer status) {
        List<Long> spuIds = loadMerchantSpuIds(merchantId);
        if (spuIds.isEmpty()) return new Page<>(page, size, 0);
        List<Long> orderIds = loadMerchantOrderIds(spuIds);
        if (orderIds.isEmpty()) return new Page<>(page, size, 0);
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>().in(Order::getId, orderIds);
        if (status != null) w.eq(Order::getStatus, status);
        w.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), w);
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> toVO(o, itemsMap)).collect(Collectors.toList());
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<String> listOrderNosByMerchant(Long merchantId) {
        List<Long> spuIds = loadMerchantSpuIds(merchantId);
        if (spuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = loadMerchantOrderIds(spuIds);
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orderMapper.selectList(new LambdaQueryWrapper<Order>().in(Order::getId, orderIds)).stream()
                .map(Order::getOrderNo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Page<OrderVO> listAll(int page, int size, Integer status) {
        Page<Order> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(pageReq, wrapper);
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> toVO(o, itemsMap)).collect(Collectors.toList());
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<Order> listForRecon(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .between(start != null && end != null, Order::getCreatedAt, start, end)
                        .orderByAsc(Order::getCreatedAt));
    }

    @Override
    @Transactional
    public void markShipped(Long id, String userType, Long merchantId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        ensureMerchantOwnsOrder(order, userType, merchantId);
        if (order.getStatus() == null || order.getStatus() != 1) {
            if (order.getStatus() != null && order.getStatus() == 2) throw new BusinessException(OrderErrorCode.ORDER_ALREADY_SHIPPED);
            if (order.getStatus() != null && order.getStatus() == 4) throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CANCELLED);
            throw new BusinessException(OrderErrorCode.ORDER_NOT_PAID);
        }
        order.setStatus(2);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status, String userType, Long merchantId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        ensureMerchantOwnsOrder(order, userType, merchantId);
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    private void ensureMerchantOwnsOrder(Order order, String userType, Long merchantId) {
        if (!"merchant".equals(userType)) {
            return;
        }
        if (merchantId == null) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        List<Long> merchantSpuIds = loadMerchantSpuIds(merchantId);
        if (merchantSpuIds.isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        final List<Long> ownedSpuIds = merchantSpuIds;
        boolean owned = !items.isEmpty() && items.stream().allMatch(item -> ownedSpuIds.contains(item.getSpuId()));
        if (!owned) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
    }

    private OrderVO toVO(Order order, Map<Long, List<OrderItem>> itemsMap) {
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

        List<OrderItem> items = itemsMap.getOrDefault(order.getId(), java.util.Collections.emptyList());
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

    private OrderSummaryVO toSummaryVO(Order order, List<OrderItem> items) {
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setOrderNo(order.getOrderNo());
        summary.setTotalAmount(order.getTotalAmount());
        summary.setStatus(order.getStatus());
        summary.setStatusText(statusText(order.getStatus()));
        summary.setCreatedAt(order.getCreatedAt());
        if (!items.isEmpty()) {
            OrderItem firstItem = items.getFirst();
            int itemCount = items.stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            String firstItemName = firstItem.getName();
            summary.setFirstItemName(firstItemName);
            summary.setItemCount(itemCount);
            summary.setItemSummary(buildItemSummary(firstItemName, itemCount));
        }
        return summary;
    }

    private List<Long> loadMerchantSpuIds(Long merchantId) {
        try {
            var response = productSpuClient.getSpuIdsByMerchant(merchantId);
            return response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> loadMerchantOrderIds(List<Long> spuIds) {
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getSpuId, spuIds));
        return items.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<Long, List<OrderItem>> loadItemsForOrders(List<Order> orders) {
        if (orders.isEmpty()) return java.util.Collections.emptyMap();
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        return items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private int normalizeSummaryLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private String buildItemSummary(String firstItemName, int itemCount) {
        if (firstItemName == null || firstItemName.isBlank()) {
            return null;
        }
        if (itemCount <= 1) {
            return firstItemName;
        }
        return firstItemName + " 等" + itemCount + "件";
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
