package com.ecommerce.payment.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment")
    public Result<List<PaymentVO>> listAll(@RequestParam(required = false) Integer status) {
        return Result.ok(paymentService.listAll(status));
    }

    @PostMapping("/payment/{orderNo}/refund")
    public Result<PaymentVO> refund(@PathVariable String orderNo,
                                     @Valid @RequestBody RefundRequest request) {
        return Result.ok(paymentService.refund(orderNo, request));
    }
}
