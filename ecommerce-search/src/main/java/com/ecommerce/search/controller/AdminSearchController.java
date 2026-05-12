package com.ecommerce.search.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSearchController {

    private final SearchService searchService;

    @PostMapping("/search/sync")
    public Result<String> syncSingle(@RequestParam Long spuId) {
        return Result.ok("ok");
    }

    @PostMapping("/search/reindex")
    public Result<String> reindex() {
        searchService.createIndex();
        return Result.ok("Index created");
    }
}
