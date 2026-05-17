package com.ecommerce.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.payment.dto.response.ReconciliationVO;

import java.util.List;

public interface ReconciliationService {
    ReconciliationVO runReconciliation();
    List<ReconciliationVO> listReconciliations();
    Page<ReconciliationVO> listReconciliations(int page, int size);
    ReconciliationVO getReconciliationDetail(Long id);
    Page<ReconciliationVO> listByMerchant(Long merchantId, int page, int size);
    ReconciliationVO getReconciliationDetailByMerchant(Long merchantId, Long id);
}
