package com.ecommerce.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.payment.dto.response.SettlementVO;

public interface SettlementService {
    SettlementVO generateSettlement(String date);
    Page<SettlementVO> listSettlements(int page, int size);
    Page<SettlementVO> listByMerchant(Long merchantId, int page, int size);
}
