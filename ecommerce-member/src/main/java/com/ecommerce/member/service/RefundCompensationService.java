package com.ecommerce.member.service;

import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;

public interface RefundCompensationService {
    RefundCompensationResult compensate(RefundCompensationRequest request);
}
