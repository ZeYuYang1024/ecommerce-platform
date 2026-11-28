package com.ecommerce.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.AfterSaleRefundRequest;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.entity.AfterSale;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.AfterSaleMapper;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.mapper.ShippingClaimMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleService {
    public static final String RELEASE_TOPIC = "after-sale-stock-release";

    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final AfterSaleMapper cases;
    private final ShippingClaimMapper shippingClaims;
    private final ShippingClaimReaper claimReaper;
    private final ProductSpuClient catalog;
    private final OutboxService outbox;

    public record BuyerStatus(boolean canApply, AfterSale application) {}

    public BuyerStatus buyerStatus(Long userId, String orderNo) {
        Order order = requireBuyer(findOrder(orderNo, false), userId);
        AfterSale application = findCase(order.getId());
        return new BuyerStatus(application == null && eligible(order), application);
    }

    @Transactional
    public AfterSale apply(Long userId, String orderNo, String reason) {
        Order order = requireBuyer(findOrder(orderNo, true), userId);
        AfterSale existing = findCase(order.getId());
        if (existing != null) return existing;
        if (!eligible(order)) throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_ELIGIBLE);

        AfterSale application = new AfterSale();
        application.setId(SnowflakeUtils.nextId());
        application.setOrderId(order.getId());
        application.setOrderNo(order.getOrderNo());
        application.setUserId(userId);
        application.setMerchantId(resolveMerchant(order.getId()));
        application.setAmount(order.getTotalAmount());
        application.setReason(reason.trim());
        application.setStatus(AfterSale.Status.PENDING);
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(application.getCreatedAt());
        cases.insert(application);
        return application;
    }

    public Page<AfterSale> list(Long merchantId, AfterSale.Status status, int page, int size) {
        return cases.selectPage(new Page<>(page, size), new LambdaQueryWrapper<AfterSale>()
                .eq(merchantId != null, AfterSale::getMerchantId, merchantId)
                .eq(status != null, AfterSale::getStatus, status)
                .orderByDesc(AfterSale::getId));
    }

    @Transactional
    public AfterSale review(Long merchantId, Long id, boolean approved, String reviewNote) {
        AfterSale application = cases.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id).last("FOR UPDATE"));
        if (application == null || (merchantId != null && !merchantId.equals(application.getMerchantId()))) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FOUND);
        }
        if (application.getStatus() != AfterSale.Status.PENDING) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_ALREADY_REVIEWED);
        }
        if (!approved && (reviewNote == null || reviewNote.isBlank())) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_REVIEW_NOTE_REQUIRED);
        }

        application.setReviewNote(reviewNote == null ? null : reviewNote.trim());
        if (approved) {
            requestRefund(application);
        } else {
            application.setStatus(AfterSale.Status.REJECTED);
            update(application);
        }
        return application;
    }

    @Transactional
    public AfterSale retryRefund(Long id) {
        AfterSale existing = cases.selectById(id);
        if (existing == null) throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FOUND);
        Order order = findOrder(existing.getOrderNo(), true);
        AfterSale application = findCase(order.getId(), true);
        if (application == null || application.getStatus() != AfterSale.Status.FAILED) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FAILED);
        }
        if (!eligible(order) && !Objects.equals(order.getStatus(), OrderStatus.REFUNDED)) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_ELIGIBLE);
        }
        requestRefund(application);
        return application;
    }

    private void requestRefund(AfterSale application) {
        application.setRefundAttempt(application.getRefundAttempt() + 1);
        application.setStatus(AfterSale.Status.REFUNDING);
        application.setFailureReason(null);
        update(application);
        outbox.enqueue("after-sale", application.getOrderNo(), AfterSaleRefundRequest.TOPIC,
                new AfterSaleRefundRequest(application.getId(), application.getOrderId(), application.getOrderNo(),
                        application.getUserId(), application.getReason(), application.getRefundAttempt(),
                        application.getAmount()));
    }

    @Transactional
    public void claimShipping(Long orderId, String shippingNo, Long shippingId, boolean managed) {
        // The order row serializes shipment claims with buyer applications.
        Order order = orders.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId).last("FOR UPDATE"));
        if (order == null || !Objects.equals(order.getStatus(), OrderStatus.PAID)) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_PAID);
        }
        AfterSale application = findCase(orderId);
        if (application != null && (application.getStatus() == AfterSale.Status.PENDING
                || application.getStatus() == AfterSale.Status.REFUNDING)) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_BLOCKS_SHIPPING);
        }
        shippingClaims.claim(orderId, shippingNo, shippingId, managed);
    }

    public void releaseShipping(Long orderId, String shippingNo) {
        shippingClaims.release(orderId, shippingNo);
    }

    public void confirmShipping(Long orderId, String shippingNo) {
        shippingClaims.confirm(orderId, shippingNo);
    }

    @Transactional
    public void finish(OrderRefundedMessage result) {
        Order order = lookupOrder(result.getOrderNo(), true);
        if (order == null) {
            finishMissingOrder(result);
            return;
        }
        AfterSale application = findCase(order.getId(), true);
        boolean correlated = result.getAfterSaleId() != null;
        if (correlated && (application == null || !application.getId().equals(result.getAfterSaleId()))) {
            log.error("Refund outcome has no matching application: orderNo={}, refundNo={}, afterSaleId={}",
                    result.getOrderNo(), result.getRefundNo(), result.getAfterSaleId());
            return;
        }
        if (correlated && application.getStatus() == AfterSale.Status.REFUNDED) return;
        boolean currentAttempt = correlated && application.getStatus() == AfterSale.Status.REFUNDING
                && application.getRefundAttempt() == Objects.requireNonNullElse(result.getRefundAttempt(), 0);
        if (!"SUCCESS".equals(result.getRefundStatus())) {
            if (currentAttempt) fail(application, result.getFailureReason());
            return;
        }
        if (!correlated && isExpectedPartial(result, order)) return;
        if (!matchesFullRefund(result, order.getUserId(), order.getTotalAmount())) {
            log.error("Refund outcome violates type, amount or buyer: orderNo={}, refundNo={}, type={}, amount={}",
                    result.getOrderNo(), result.getRefundNo(), result.getRefundType(), result.getRefundAmount());
            if (currentAttempt) fail(application, "退款结果的类型、金额或买家与订单不一致");
            return;
        }

        if (Objects.equals(order.getStatus(), OrderStatus.REFUNDED)) {
            recordRefund(application, result, null);
            return;
        }
        boolean migrated = Objects.equals(order.getStatus(), OrderStatus.PAID)
                && orders.transitionStatus(order.getId(), OrderStatus.PAID, OrderStatus.REFUNDED);
        if (!migrated) {
            log.error("Refund succeeded but order state could not be migrated: orderNo={}, refundNo={}, orderStatus={}",
                    result.getOrderNo(), result.getRefundNo(), order.getStatus());
            recordRefund(application, result, "退款已成功，订单状态迁移失败，库存未自动释放");
            return;
        }
        if (claimReaper.hasShipping(order.getId())) {
            log.error("Refund succeeded with a shipment claim present: orderNo={}, refundNo={}",
                    result.getOrderNo(), result.getRefundNo());
            recordRefund(application, result, "退款已成功，订单存在发货记录，库存未自动释放");
            return;
        }

        recordRefund(application, result, null);
        List<OrderItemMessage> release = orderItems(order.getId()).stream()
                .map(item -> new OrderItemMessage(item.getSkuId(), item.getQuantity())).toList();
        outbox.enqueue("after-sale", order.getOrderNo(), RELEASE_TOPIC,
                new OrderInventoryMessage(order.getOrderNo(), release));
    }

    private void finishMissingOrder(OrderRefundedMessage result) {
        log.error("Refund outcome has no matching order: orderNo={}, refundNo={}", result.getOrderNo(), result.getRefundNo());
        if (result.getAfterSaleId() == null) return;
        AfterSale application = cases.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, result.getAfterSaleId())
                .eq(AfterSale::getOrderNo, result.getOrderNo()).last("FOR UPDATE"));
        if (application == null || application.getStatus() != AfterSale.Status.REFUNDING
                || application.getRefundAttempt() != Objects.requireNonNullElse(result.getRefundAttempt(), 0)) return;
        if ("SUCCESS".equals(result.getRefundStatus())
                && matchesFullRefund(result, application.getUserId(), application.getAmount())) {
            recordRefund(application, result, "退款已成功，订单不存在，库存未自动释放");
        } else {
            fail(application, "无法核对退款结果，订单不存在");
        }
    }

    private boolean isExpectedPartial(OrderRefundedMessage result, Order order) {
        return "PARTIAL".equals(result.getRefundType()) && Objects.equals(result.getUserId(), order.getUserId())
                && result.getRefundAmount() != null && order.getTotalAmount() != null
                && result.getRefundAmount().signum() > 0 && result.getRefundAmount().compareTo(order.getTotalAmount()) < 0;
    }

    private boolean matchesFullRefund(OrderRefundedMessage result, Long userId, BigDecimal total) {
        return "FULL".equals(result.getRefundType()) && Objects.equals(result.getUserId(), userId)
                && total != null && total.signum() > 0 && result.getRefundAmount() != null
                && total.compareTo(result.getRefundAmount()) == 0
                && result.getRefundNo() != null && !result.getRefundNo().isBlank();
    }

    private void recordRefund(AfterSale application, OrderRefundedMessage result, String warning) {
        if (application == null || application.getStatus() == AfterSale.Status.REJECTED
                || application.getStatus() == AfterSale.Status.REFUNDED) return;
        application.setStatus(AfterSale.Status.REFUNDED);
        application.setRefundNo(result.getRefundNo());
        application.setFailureReason(warning);
        update(application);
    }

    private void fail(AfterSale application, String reason) {
        application.setStatus(AfterSale.Status.FAILED);
        application.setFailureReason(reason == null ? "退款处理失败" : reason);
        update(application);
    }

    private void update(AfterSale application) {
        application.setUpdatedAt(LocalDateTime.now());
        cases.updateById(application);
    }

    private Order findOrder(String orderNo, boolean lock) {
        Order order = lookupOrder(orderNo, lock);
        if (order == null) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    private Order lookupOrder(String orderNo, boolean lock) {
        return orders.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).last(lock, "FOR UPDATE"));
    }

    private Order requireBuyer(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    private AfterSale findCase(Long orderId) {
        return findCase(orderId, false);
    }

    private AfterSale findCase(Long orderId, boolean lock) {
        return cases.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, orderId).last(lock, "FOR UPDATE"));
    }

    private boolean eligible(Order order) {
        return Objects.equals(order.getStatus(), OrderStatus.PAID) && order.getTotalAmount() != null
                && order.getTotalAmount().signum() > 0 && !claimReaper.hasShipping(order.getId());
    }

    private List<OrderItem> orderItems(Long orderId) {
        return items.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    private Long resolveMerchant(Long orderId) {
        List<Long> skuIds = orderItems(orderId).stream().map(OrderItem::getSkuId).distinct().toList();
        if (skuIds.isEmpty() || skuIds.contains(null)) return null;
        var reply = catalog.batchQuerySkus(skuIds);
        if (reply == null || reply.getCode() != 200 || reply.getData() == null) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_CATALOG_UNAVAILABLE);
        }
        List<SkuBatchVO> rows = reply.getData();
        if (!rows.stream().map(SkuBatchVO::getSkuId).toList().containsAll(skuIds)) return null;
        List<Long> merchants = rows.stream().map(SkuBatchVO::getMerchantId).distinct().toList();
        // Platform orders, mixed baskets and retired SKUs go to the platform queue.
        return merchants.size() == 1 ? merchants.getFirst() : null;
    }
}
