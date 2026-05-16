package com.ecommerce.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.dto.request.UpdateDocumentRequest;
import com.ecommerce.knowledge.dto.response.DocumentVO;
import com.ecommerce.knowledge.service.KbDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/knowledge/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService documentService;

    @PostMapping
    public Result<DocumentVO> create(@RequestBody CreateDocumentRequest request) {
        return Result.ok(documentService.create(request));
    }

    @PutMapping("/{id}")
    public Result<DocumentVO> update(@PathVariable Long id, @RequestBody UpdateDocumentRequest request) {
        return Result.ok(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        return Result.ok(documentService.getById(id));
    }

    @GetMapping
    public Result<Page<DocumentVO>> page(@RequestParam(name = "page", defaultValue = "1") int pageNum,
                                          @RequestParam(name = "size", defaultValue = "20") int pageSize,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String status) {
        return Result.ok(documentService.page(pageNum, pageSize, categoryId, status));
    }

    @PostMapping("/{id}/reindex")
    public Result<Void> reindex(@PathVariable Long id) {
        documentService.reindex(id);
        return Result.ok();
    }
}
