package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.response.SettlementVO;

import java.util.List;

public interface SettlementService {
    SettlementVO generateSettlement(String date);
    List<SettlementVO> listSettlements();
}
