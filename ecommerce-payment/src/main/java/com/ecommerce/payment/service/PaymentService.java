package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;

import java.util.List;

public interface PaymentService {
    PaymentVO pay(Long userId, PayRequest request);
    PaymentVO queryByOrderNo(String orderNo);
    PaymentVO refund(String orderNo, RefundRequest request);
    List<PaymentVO> listAll(Integer status);
}
