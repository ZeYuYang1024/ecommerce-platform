package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.response.ReconciliationVO;

import java.util.List;

public interface ReconciliationService {
    ReconciliationVO runReconciliation();
    List<ReconciliationVO> listReconciliations();
    ReconciliationVO getReconciliationDetail(Long id);
}
