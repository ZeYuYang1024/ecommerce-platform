package com.ecommerce.search.dto.response;

import com.ecommerce.search.entity.ProductDocument;
import lombok.Data;
import java.util.List;

@Data
public class SearchResponse {
    private List<ProductDocument> records;
    private long total;
    private int page;
    private int size;
}
