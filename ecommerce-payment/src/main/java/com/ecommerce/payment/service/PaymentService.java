package com.ecommerce.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;

public interface PaymentService {
    PaymentVO pay(Long userId, PayRequest request);
    PaymentVO queryByOrderNo(String orderNo);
    PaymentVO queryByOrderNoForUser(Long userId, String orderNo);
    PaymentVO refund(String orderNo, RefundRequest request);
    Page<PaymentVO> listAll(Integer status, int page, int size);
    Page<PaymentVO> listByMerchant(Long merchantId, Integer status, int page, int size);
    PaymentVO refundByMerchant(Long merchantId, String orderNo, RefundRequest request);
}
