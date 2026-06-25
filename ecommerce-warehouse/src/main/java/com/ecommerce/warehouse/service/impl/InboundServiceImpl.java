package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.InboundStatus;
import com.ecommerce.warehouse.common.InboundType;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateInboundRequest;
import com.ecommerce.warehouse.dto.response.InboundOrderVO;
import com.ecommerce.warehouse.entity.InboundOrder;
import com.ecommerce.warehouse.entity.InboundOrderItem;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.InboundOrderItemMapper;
import com.ecommerce.warehouse.mapper.InboundOrderMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.service.InboundService;
import com.ecommerce.warehouse.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InboundServiceImpl implements InboundService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockService stockService;

    public InboundServiceImpl(InboundOrderMapper inboundOrderMapper,
                              InboundOrderItemMapper inboundOrderItemMapper,
                              WarehouseMapper warehouseMapper,
                              StockService stockService) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockService = stockService;
    }

    @Override
    public IPage<InboundOrderVO> listInbounds(int page, int size, Long warehouseId, Long merchantId) {
        Page<InboundOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<InboundOrder>()
                .orderByDesc(InboundOrder::getCreatedAt);
        if (warehouseId != null) {
            wrapper.eq(InboundOrder::getWarehouseId, warehouseId);
        }
        if (merchantId != null) {
            wrapper.eq(InboundOrder::getMerchantId, merchantId);
        }
        IPage<InboundOrder> result = inboundOrderMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public InboundOrderVO getInbound(Long id) {
        InboundOrder entity = inboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.INBOUND_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public InboundOrderVO createInbound(CreateInboundRequest req) {
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouse.getStatus() != null && warehouse.getStatus() != 1) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_DISABLED);
        }

        InboundOrder order = new InboundOrder();
        order.setInboundNo(generateInboundNo());
        order.setWarehouseId(req.getWarehouseId());
        order.setInboundType(req.getInboundType());
        order.setSourceOrderNo(req.getSourceOrderNo());
        order.setStatus(InboundStatus.PENDING);
        order.setMerchantId(req.getMerchantId());
        order.setRemark(req.getRemark());
        inboundOrderMapper.insert(order);

        if (req.getItems() != null) {
            for (CreateInboundRequest.InboundItem item : req.getItems()) {
                InboundOrderItem orderItem = new InboundOrderItem();
                orderItem.setInboundId(order.getId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setReceivedQty(0);
                inboundOrderItemMapper.insert(orderItem);
            }
        }

        return toVO(order);
    }

    @Override
    @Transactional
    public void confirmReceived(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.INBOUND_NOT_FOUND);
        }
        if (order.getStatus() != InboundStatus.PENDING) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }
        order.setStatus(InboundStatus.RECEIVED);
        inboundOrderMapper.updateById(order);

        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(
                new LambdaQueryWrapper<InboundOrderItem>()
                        .eq(InboundOrderItem::getInboundId, id));
        for (InboundOrderItem item : items) {
            item.setReceivedQty(item.getQuantity());
            inboundOrderItemMapper.updateById(item);
        }
    }

    @Override
    @Transactional
    public void confirmShelved(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.INBOUND_NOT_FOUND);
        }
        if (order.getStatus() != InboundStatus.RECEIVED) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }

        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(
                new LambdaQueryWrapper<InboundOrderItem>()
                        .eq(InboundOrderItem::getInboundId, id));
        for (InboundOrderItem item : items) {
            if (item.getBinId() == null) {
                throw new BusinessException(WarehouseErrorCode.BIN_NOT_FOUND);
            }
            stockService.addStock(order.getWarehouseId(), item.getSkuId(), item.getBinId(), item.getQuantity());
        }

        order.setStatus(InboundStatus.SHELVED);
        inboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void completeInbound(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(WarehouseErrorCode.INBOUND_NOT_FOUND);
        }
        if (order.getStatus() != InboundStatus.SHELVED) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }
        order.setStatus(InboundStatus.COMPLETED);
        inboundOrderMapper.updateById(order);
    }

    private String generateInboundNo() {
        return "IN" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private InboundOrderVO toVO(InboundOrder entity) {
        InboundOrderVO vo = new InboundOrderVO();
        vo.setId(entity.getId());
        vo.setInboundNo(entity.getInboundNo());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setInboundType(entity.getInboundType());
        vo.setInboundTypeText(InboundType.text(entity.getInboundType()));
        vo.setSourceOrderNo(entity.getSourceOrderNo());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(InboundStatus.text(entity.getStatus()));
        vo.setMerchantId(entity.getMerchantId());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());

        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(
                new LambdaQueryWrapper<InboundOrderItem>()
                        .eq(InboundOrderItem::getInboundId, entity.getId()));
        List<InboundOrderVO.Item> voItems = new ArrayList<>();
        if (items != null) {
            for (InboundOrderItem item : items) {
                InboundOrderVO.Item voItem = new InboundOrderVO.Item();
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
}
