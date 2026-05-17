package com.ecommerce.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.dto.request.UpdateDocumentRequest;
import com.ecommerce.knowledge.dto.response.DocumentVO;
import com.ecommerce.knowledge.service.KbDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchant/knowledge/documents")
@RequiredArgsConstructor
public class MerchantKbDocumentController {

    private final KbDocumentService documentService;

    @PostMapping
    public Result<DocumentVO> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                                     @Valid @RequestBody CreateDocumentRequest request) {
        return Result.ok(documentService.createForMerchant(merchantId, request));
    }

    @PutMapping("/{id}")
    public Result<DocumentVO> update(@RequestHeader("X-Merchant-Id") Long merchantId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateDocumentRequest request) {
        return Result.ok(documentService.updateForMerchant(merchantId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @PathVariable Long id) {
        documentService.deleteForMerchant(merchantId, id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@RequestHeader("X-Merchant-Id") Long merchantId,
                                      @PathVariable Long id) {
        return Result.ok(documentService.getForMerchant(merchantId, id));
    }

    @GetMapping
    public Result<Page<DocumentVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                         @RequestParam(name = "page", defaultValue = "1") int pageNum,
                                         @RequestParam(name = "size", defaultValue = "20") int pageSize,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String status) {
        return Result.ok(documentService.pageForMerchant(merchantId, pageNum, pageSize, categoryId, status));
    }

    @PostMapping("/{id}/reindex")
    public Result<Void> reindex(@RequestHeader("X-Merchant-Id") Long merchantId,
                                @PathVariable Long id) {
        documentService.reindexForMerchant(merchantId, id);
        return Result.ok();
    }
}
