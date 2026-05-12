package com.ecommerce.search.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.search.dto.request.SearchRequest;
import com.ecommerce.search.dto.response.SearchResponse;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public Result<SearchResponse> search(SearchRequest request) {
        return Result.ok(searchService.search(request));
    }
}
