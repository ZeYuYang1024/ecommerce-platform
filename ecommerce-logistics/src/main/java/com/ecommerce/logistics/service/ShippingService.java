package com.ecommerce.logistics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.response.FulfillmentSummaryVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.TrackingVO;

import java.util.List;

public interface ShippingService {
    ShippingOrderVO createShipping(CreateShippingRequest request, String userType, Long merchantId);
    IPage<ShippingOrderVO> listShipping(int page, int size, String orderNo, Integer shippingStatus, Long merchantId);
    ShippingOrderVO getShipping(Long id, String userType, Long merchantId);
    ShippingOrderVO getShippingByOrderId(Long orderId);
    TrackingVO getTracking(Long shippingId, Long merchantId);
    TrackingVO getTrackingByTrackingNo(String trackingNo, String providerCode, Long merchantId);
    List<FulfillmentSummaryVO> getFulfillmentSummary(List<Long> orderIds);
    void processCallback(String aggregationProviderCode, String rawBody, String signature);
    String generateWaybill(Long shippingId, Long merchantId);
    List<ShippingOrderVO> batchShip(com.ecommerce.logistics.dto.request.BatchShipRequest request, String userType, Long merchantId);
}
