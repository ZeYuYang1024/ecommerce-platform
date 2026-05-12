package com.ecommerce.search.service;

import com.ecommerce.search.dto.request.SearchRequest;
import com.ecommerce.search.dto.response.SearchResponse;
import com.ecommerce.search.entity.ProductDocument;
import java.util.List;

public interface SearchService {
    void createIndex();
    void indexProduct(ProductDocument doc);
    void bulkIndex(List<ProductDocument> docs);
    void deleteProduct(String id);
    SearchResponse search(SearchRequest request);
}
