package com.ecommerce.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.search.common.SearchErrorCode;
import com.ecommerce.search.dto.request.SearchRequest;
import com.ecommerce.search.dto.response.SearchResponse;
import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String INDEX = "products";

    private final ElasticsearchClient es;

    @Override
    public void createIndex() {
        try {
            ExistsRequest exists = ExistsRequest.of(e -> e.index(INDEX));
            if (es.indices().exists(exists).value()) return;
            CreateIndexResponse rsp = es.indices().create(c -> c.index(INDEX));
            log.info("ES index created: {}, acknowledged={}", INDEX, rsp.acknowledged());
        } catch (Exception e) {
            log.error("Failed to create ES index", e);
        }
    }

    @Override
    public void indexProduct(ProductDocument doc) {
        try {
            es.index(IndexRequest.of(i -> i.index(INDEX).id(doc.getId()).document(doc)));
        } catch (Exception e) {
            log.error("Failed to index product: id={}", doc.getId(), e);
            throw new BusinessException(SearchErrorCode.INDEX_ERROR);
        }
    }

    @Override
    public void bulkIndex(List<ProductDocument> docs) {
        if (docs.isEmpty()) return;
        try {
            List<BulkOperation> ops = docs.stream()
                .map(d -> BulkOperation.of(o -> o.index(i -> i.index(INDEX).id(d.getId()).document(d))))
                .toList();
            es.bulk(BulkRequest.of(b -> b.operations(ops)));
            log.info("Bulk indexed {} products", docs.size());
        } catch (Exception e) {
            log.error("Bulk index failed", e);
            throw new BusinessException(SearchErrorCode.INDEX_ERROR);
        }
    }

    @Override
    public void deleteProduct(String id) {
        try {
            es.delete(DeleteRequest.of(d -> d.index(INDEX).id(id)));
        } catch (Exception e) {
            log.error("Failed to delete product: id={}", id, e);
        }
    }

    @Override
    public SearchResponse search(SearchRequest req) {
        try {
            int from = (req.getPage() - 1) * req.getSize();
            SearchResponse rsp = new SearchResponse();
            rsp.setPage(req.getPage());
            rsp.setSize(req.getSize());

            boolean hasKeyword = req.getKeyword() != null && !req.getKeyword().isBlank();
            boolean hasCategory = req.getCategoryId() != null && req.getCategoryId() > 0;

            var sr = es.search(s -> {
                s.index(INDEX)
                 .from(from)
                 .size(req.getSize())
                 .query(q -> {
                     if (!hasKeyword && !hasCategory) {
                         return q.matchAll(m -> m);
                     }
                     return q.bool(b -> {
                         if (hasKeyword) {
                             b.must(m -> m.multiMatch(mm -> mm
                                 .fields("name", "description")
                                 .query(req.getKeyword())));
                         }
                         if (hasCategory) {
                             b.filter(f -> f.term(t -> t.field("categoryId").value(req.getCategoryId())));
                         }
                         return b;
                     });
                 });

                String sort = req.getSort() != null ? req.getSort() : "";
                switch (sort) {
                    case "price_asc"  -> s.sort(so -> so.field(f -> f.field("minPrice").order(SortOrder.Asc)));
                    case "price_desc" -> s.sort(so -> so.field(f -> f.field("maxPrice").order(SortOrder.Desc)));
                    case "rating"     -> s.sort(so -> so.field(f -> f.field("avgRating").order(SortOrder.Desc)));
                    case "sales"      -> s.sort(so -> so.field(f -> f.field("salesCount").order(SortOrder.Desc)));
                    default           -> s.sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                }
                return s;
            }, ProductDocument.class);

            rsp.setTotal(sr.hits().total() != null ? sr.hits().total().value() : 0);
            rsp.setRecords(sr.hits().hits().stream()
                .map(h -> h.source())
                .toList());
            return rsp;
        } catch (Exception e) {
            log.error("Search failed", e);
            throw new BusinessException(SearchErrorCode.SEARCH_ERROR);
        }
    }
}
