package com.ecommerce.payment.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public Result<PaymentVO> pay(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody PayRequest request) {
        return Result.ok(paymentService.pay(userId, request));
    }

    @GetMapping("/{orderNo}")
    public Result<PaymentVO> query(@PathVariable String orderNo) {
        return Result.ok(paymentService.queryByOrderNo(orderNo));
    }
}
