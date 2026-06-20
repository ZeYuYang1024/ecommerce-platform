package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OutboundShippedMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.OutboundStatus;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.response.OutboundOrderVO;
import com.ecommerce.warehouse.entity.OutboundOrder;
import com.ecommerce.warehouse.entity.OutboundOrderItem;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.OutboundOrderItemMapper;
import com.ecommerce.warehouse.mapper.OutboundOrderMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.service.OutboundService;
import com.ecommerce.warehouse.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OutboundServiceImpl implements OutboundService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockService stockService;
    private final OutboxService outboxService;

    public OutboundServiceImpl(OutboundOrderMapper outboundOrderMapper,
                               OutboundOrderItemMapper outboundOrderItemMapper,
                               WarehouseMapper warehouseMapper,
                               StockService stockService,
                               OutboxService outboxService) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockService = stockService;
        this.outboxService = outboxService;
    }

    // ======================== Query ========================

    @Override
    public IPage<OutboundOrderVO> listOutbounds(int page, int size, Long warehouseId, Long merchantId) {
        Page<OutboundOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<OutboundOrder>()
                .orderByDesc(OutboundOrder::getCreatedAt);
        if (warehouseId != null) {
            wrapper.eq(OutboundOrder::getWarehouseId, warehouseId);
        }
        if (merchantId != null) {
            wrapper.eq(OutboundOrder::getMerchantId, merchantId);
        }
        IPage<OutboundOrder> result = outboundOrderMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public OutboundOrderVO getOutbound(Long id) {
        OutboundOrder entity = outboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.OUTBOUND_NOT_FOUND);
        }
        return toVO(entity);
    }

    // ======================== Mutations ========================

    @Override
    @Transactional
    public OutboundOrderVO createOutbound(CreateOutboundRequest req) {
        // Validate warehouse
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouse.getStatus() != null && warehouse.getStatus() != 1) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_DISABLED);
        }
        if (!"MANAGED".equals(warehouse.getStockMode())) {
            throw new BusinessException(WarehouseErrorCode.NOT_MANAGED_WAREHOUSE);
        }

        // Lock stock for each item
        if (req.getItems() != null) {
            for (CreateOutboundRequest.OutboundItem item : req.getItems()) {
                stockService.lockStock(req.getWarehouseId(), item.getSkuId(), item.getQuantity());
            }
        }

        // Create outbound order
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo(generateOutboundNo());
        order.setWarehouseId(req.getWarehouseId());
        order.setOutboundType(req.getOutboundType());
        order.setShippingId(req.getShippingId());
        order.setStatus(OutboundStatus.PENDING);
        order.setMerchantId(req.getMerchantId());
        order.setRemark(req.getRemark());
        outboundOrderMapper.insert(order);

        // Create outbound items
        if (req.getItems() != null) {
            for (CreateOutboundRequest.OutboundItem item : req.getItems()) {
                OutboundOrderItem orderItem = new OutboundOrderItem();
                orderItem.setOutboundId(order.getId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPickedQty(0);
                orderItem.setShippedQty(0);
                orderItem.setBinId(item.getBinId());
                outboundOrderItemMapper.insert(orderItem);
            }
        }

        return toVO(order);
    }

    @Override
    @Transactional
    public void startPicking(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.OUTBOUND_NOT_FOUND);
        }
        if (order.getStatus() != OutboundStatus.PENDING) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }
        order.setStatus(OutboundStatus.PICKING);
        outboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void confirmShipped(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.OUTBOUND_NOT_FOUND);
        }
        if (order.getStatus() != OutboundStatus.PICKING) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Deduct stock for each item
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(
                new LambdaQueryWrapper<OutboundOrderItem>()
                        .eq(OutboundOrderItem::getOutboundId, id));
        for (OutboundOrderItem item : items) {
            stockService.deductStock(order.getWarehouseId(), item.getSkuId(), item.getQuantity());
            item.setShippedQty(item.getQuantity());
            outboundOrderItemMapper.updateById(item);
        }

        order.setStatus(OutboundStatus.SHIPPED);
        outboundOrderMapper.updateById(order);

        // Send outbound-shipped message via outbox
        OutboundShippedMessage message = new OutboundShippedMessage();
        message.setShippingId(order.getShippingId() != null ? order.getShippingId() : 0L);
        message.setOutboundId(order.getId());
        message.setWarehouseId(order.getWarehouseId());
        message.setOccurredAt(LocalDateTime.now());
        outboxService.enqueue("outbound_order", order.getOutboundNo(),
                "outbound-shipped", message);
    }

    @Override
    @Transactional
    public void confirmDelivered(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.OUTBOUND_NOT_FOUND);
        }
        if (order.getStatus() != OutboundStatus.SHIPPED) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }
        order.setStatus(OutboundStatus.DELIVERED);
        outboundOrderMapper.updateById(order);
    }

    // ======================== Private helpers ========================

    private String generateOutboundNo() {
        return "OUT" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private OutboundOrderVO toVO(OutboundOrder entity) {
        OutboundOrderVO vo = new OutboundOrderVO();
        vo.setId(entity.getId());
        vo.setOutboundNo(entity.getOutboundNo());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setOutboundType(entity.getOutboundType());
        vo.setOutboundTypeText(outboundTypeText(entity.getOutboundType()));
        vo.setShippingId(entity.getShippingId());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(OutboundStatus.text(entity.getStatus()));
        vo.setMerchantId(entity.getMerchantId());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());

        // Load items
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(
                new LambdaQueryWrapper<OutboundOrderItem>()
                        .eq(OutboundOrderItem::getOutboundId, entity.getId()));
        List<OutboundOrderVO.Item> voItems = new ArrayList<>();
        if (items != null) {
            for (OutboundOrderItem item : items) {
                OutboundOrderVO.Item voItem = new OutboundOrderVO.Item();
                voItem.setId(item.getId());
                voItem.setSkuId(item.getSkuId());
                voItem.setQuantity(item.getQuantity());
                voItem.setBinId(item.getBinId());
                voItems.add(voItem);
            }
        }
        vo.setItems(voItems);
        return vo;
    }

    private String outboundTypeText(String outboundType) {
        if (outboundType == null) return null;
        return switch (outboundType) {
            case "SALES" -> "销售出库";
            case "TRANSFER" -> "调拨出库";
            case "RETURN" -> "退货出库";
            case "CHECK_OUT" -> "盘点出库";
            default -> outboundType;
        };
    }
}
