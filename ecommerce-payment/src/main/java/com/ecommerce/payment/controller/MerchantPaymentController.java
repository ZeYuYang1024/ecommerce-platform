package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchant")
public class MerchantPaymentController {

    private final PaymentService paymentService;

    public MerchantPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment")
    public Result<Page<PaymentVO>> listByMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(paymentService.listByMerchant(merchantId, status, page, size));
    }

    @PostMapping("/payment/{orderNo}/refund")
    public Result<PaymentVO> refundByMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                              @PathVariable String orderNo,
                                              @Valid @RequestBody RefundRequest request) {
        return Result.ok(paymentService.refundByMerchant(merchantId, orderNo, request));
    }
}
