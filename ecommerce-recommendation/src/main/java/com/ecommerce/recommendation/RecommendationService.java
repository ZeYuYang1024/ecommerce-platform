package com.ecommerce.recommendation;

import com.ecommerce.common.dto.RecommendationProduct;
import com.ecommerce.common.dto.RecommendationSignal;
import com.ecommerce.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
class RecommendationService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private static final Duration REQUEST_TTL = Duration.ofMinutes(30);
    private final RecommendationRepository repository;
    private final ProductClient products;
    private final InventoryClient inventory;
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    RecommendationService(RecommendationRepository repository, ProductClient products, InventoryClient inventory,
                          StringRedisTemplate redis, JsonMapper json) {
        this.repository = repository;
        this.products = products;
        this.inventory = inventory;
        this.redis = redis;
        this.json = json;
    }

    Feed feed(Long userId, int limit) {
        List<RecommendationRepository.Behavior> history = userId == null ? List.of()
                : cached("history:" + userId, RecommendationRepository.Behavior[].class, () -> repository.recent(userId));
        List<RecommendationRepository.CategoryScore> categoryScores = userId == null ? List.of()
                : cached("categories:" + userId, RecommendationRepository.CategoryScore[].class,
                        () -> repository.categories(userId));
        var taste = taste(history, categoryScores);
        List<Long> seeds = history.stream().map(RecommendationRepository.Behavior::spuId).distinct()
                .limit(12).sorted().toList();
        List<RecommendationRepository.Related> relatedProducts = seeds.isEmpty() ? List.of()
                : cached("related:" + seeds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                        RecommendationRepository.Related[].class, () -> repository.related(seeds));
        var related = relatedProducts.stream().collect(Collectors.toMap(
                RecommendationRepository.Related::id, RecommendationRepository.Related::affinity));
        List<Long> categories = taste.categories().entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3).map(Map.Entry::getKey).toList();
        var query = new RecommendationProduct.Query(relatedProducts.stream().map(RecommendationRepository.Related::id)
                .toList(), categories, true);
        List<RecommendationProduct> candidates = available(candidates(query)).stream()
                .sorted(Comparator.comparingDouble((RecommendationProduct product) ->
                        score(product, taste, related)).reversed())
                .toList();
        List<Card> cards = diversify(candidates, categories, limit).stream().map(Card::from).toList();
        return new Feed(userId == null ? null : issueRequest(userId, cards), !categories.isEmpty() || !related.isEmpty(),
                cards);
    }

    void recordBrowserEvents(long userId, List<BrowserEvent> events) {
        Instant now = Instant.now();
        List<Long> views = events.stream().filter(event -> event.type() == RecommendationEventType.VIEW)
                .map(BrowserEvent::spuId).distinct().toList();
        Map<Long, RecommendationProduct> viewed = views.isEmpty() ? Map.of() : lookup(views);
        boolean changed = false;
        Map<UUID, IssuedRequest> requests = new HashMap<>();
        for (BrowserEvent event : events) {
            switch (event.type()) {
                case VIEW -> {
                    RecommendationProduct product = viewed.get(event.spuId());
                    if (product == null) continue;
                    String id = "view:" + userId + ":" + event.spuId() + ":" + now.getEpochSecond() / 300;
                    changed |= store(id, userId, event.spuId(), event.type(), null, product, now);
                }
                case IMPRESSION, CLICK -> {
                    if (event.requestId() == null) continue;
                    IssuedRequest issued = requests.computeIfAbsent(event.requestId(), this::issuedRequest);
                    if (issued.userId() != userId || !issued.spuIds().contains(Long.toString(event.spuId()))) continue;
                    String requestId = event.requestId().toString();
                    String id = event.type().name() + ":" + requestId + ":" + event.spuId();
                    store(id, userId, event.spuId(), event.type(), requestId, null, now);
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported browser event");
            }
        }
        if (changed) invalidateHistory(userId);
    }

    void recordSignal(RecommendationSignal signal) {
        Instant now = Instant.ofEpochMilli(signal.occurredAtMillis());
        RecommendationEventType type = RecommendationEventType.valueOf(signal.type().name());
        List<Long> ids = signal.spuIds().stream().distinct().toList();
        for (int start = 0; start < ids.size(); start += 100) {
            List<Long> batch = ids.subList(start, Math.min(start + 100, ids.size()));
            Map<Long, RecommendationProduct> metadata = lookup(batch);
            for (long spuId : batch) {
                RecommendationProduct product = metadata.get(spuId);
                if (type == RecommendationEventType.CART_ADD
                        && (product == null || !product.skuIds().contains(signal.skuId()))) continue;
                store(type.name() + ":" + signal.eventId() + ":" + spuId, signal.userId(), spuId, type, null, product, now);
            }
        }
        invalidateHistory(signal.userId());
    }

    private boolean store(String id, long userId, long spuId, RecommendationEventType type, String requestId,
                          RecommendationProduct product, Instant now) {
        return repository.store(id, userId, spuId, type, requestId, product == null ? null : product.categoryId(),
                product == null ? null : product.brandId(), product == null ? null : product.minPrice(), now);
    }

    private Map<Long, RecommendationProduct> lookup(List<Long> ids) {
        return candidates(new RecommendationProduct.Query(ids, List.of(), false)).stream()
                .collect(Collectors.toMap(RecommendationProduct::id, product -> product));
    }

    private List<RecommendationProduct> candidates(RecommendationProduct.Query query) {
        return data(products.candidates(query));
    }

    private List<RecommendationProduct> available(List<RecommendationProduct> candidates) {
        List<Long> skuIds = candidates.stream().flatMap(product -> product.skuIds().stream()).distinct().toList();
        if (skuIds.isEmpty()) return List.of();
        Set<Long> availableSkuIds = new HashSet<>();
        for (int start = 0; start < skuIds.size(); start += 500) {
            var batch = skuIds.subList(start, Math.min(start + 500, skuIds.size()));
            data(inventory.stock(batch)).stream()
                    .filter(stock -> stock.availableStock() != null && stock.availableStock() > 0)
                    .map(InventoryClient.Stock::skuId).forEach(availableSkuIds::add);
        }
        return candidates.stream().filter(product -> product.skuIds().stream().anyMatch(availableSkuIds::contains))
                .toList();
    }

    private <T> T data(Result<T> result) {
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Recommendation catalog is unavailable");
        }
        return result.getData();
    }

    private <T> List<T> cached(String name, Class<T[]> type, Supplier<List<T>> load) {
        String key = "recommendation:" + name;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return List.of(json.readValue(cached, type));
        } catch (RuntimeException error) {
            log.debug("Recommendation cache read failed", error);
        }
        List<T> rows = load.get();
        try {
            redis.opsForValue().set(key, json.writeValueAsString(rows), CACHE_TTL);
        } catch (RuntimeException error) {
            log.debug("Recommendation cache write failed", error);
        }
        return rows;
    }

    private void invalidateHistory(long userId) {
        try {
            redis.delete("recommendation:history:" + userId);
        } catch (RuntimeException error) {
            log.debug("Recommendation history cache invalidation failed", error);
        }
    }

    private String issueRequest(long userId, List<Card> cards) {
        if (cards.isEmpty()) return null;
        String requestId = UUID.randomUUID().toString();
        try {
            String value = userId + "," + cards.stream().map(Card::id).collect(Collectors.joining(","));
            redis.opsForValue().set("recommendation:request:" + requestId, value, REQUEST_TTL);
            return requestId;
        } catch (RuntimeException error) {
            log.debug("Recommendation impression tracking is unavailable", error);
            return null;
        }
    }

    private IssuedRequest issuedRequest(UUID requestId) {
        String value = redis.opsForValue().get("recommendation:request:" + requestId);
        if (value == null) return new IssuedRequest(0, Set.of());
        String[] parts = value.split(",", 2);
        return new IssuedRequest(Long.parseLong(parts[0]), Set.of(parts[1].split(",")));
    }

    private Taste taste(List<RecommendationRepository.Behavior> history,
                        List<RecommendationRepository.CategoryScore> offline) {
        var categories = new HashMap<Long, Double>();
        var brands = new HashMap<Long, Double>();
        var seenProducts = new HashSet<Long>();
        var dailySignals = new HashSet<DailySignal>();
        double priceTotal = 0;
        double priceWeight = 0;
        Instant now = Instant.now();
        for (var event : history) {
            seenProducts.add(event.spuId());
            if (!dailySignals.add(new DailySignal(event.spuId(), event.type(),
                    event.occurredAt().getEpochSecond() / 86400))) continue;
            double daysAgo = Math.max(0, Duration.between(event.occurredAt(), now).toHours() / 24.0);
            double weight = event.type().weight * Math.pow(0.5, daysAgo / 7);
            if (event.categoryId() != null) categories.merge(event.categoryId(), weight, Double::sum);
            if (event.brandId() != null) brands.merge(event.brandId(), weight, Double::sum);
            if (event.price() != null) {
                priceTotal += event.price().doubleValue() * weight;
                priceWeight += weight;
            }
        }
        // Cap the offline contribution so yesterday's batch cannot drown out a fresh add-to-cart.
        offline.forEach(item -> categories.merge(item.categoryId(),
                Math.min(1.5, item.score7d() * 0.04 + item.score30d() * 0.02), Double::sum));
        return new Taste(categories, brands, seenProducts, priceWeight == 0 ? 0 : priceTotal / priceWeight,
                categories.values().stream().mapToDouble(Double::doubleValue).max().orElse(1),
                brands.values().stream().mapToDouble(Double::doubleValue).max().orElse(1));
    }

    private double score(RecommendationProduct product, Taste taste, Map<Long, Double> related) {
        double category = taste.categories().getOrDefault(product.categoryId(), 0.0) / taste.maxCategory();
        double brand = taste.brands().getOrDefault(product.brandId(), 0.0) / taste.maxBrand();
        double affinity = related.getOrDefault(product.id(), 0.0);
        double price = 0;
        if ((category > 0 || affinity > 0) && taste.price() > 0 && product.minPrice() != null) {
            double distance = Math.abs(taste.price() - product.minPrice().doubleValue());
            price = 1 / (1 + distance / taste.price());
        }
        return (category * 0.60 + brand * 0.12 + affinity * 0.22 + price * 0.06)
                * (taste.seenProducts().contains(product.id()) ? 0.75 : 1);
    }

    private List<RecommendationProduct> diversify(List<RecommendationProduct> ranked, List<Long> categories, int limit) {
        if (categories.isEmpty()) return ranked.stream().limit(limit).toList();
        Long primary = categories.getFirst();
        var exploration = ranked.stream().filter(product -> !primary.equals(product.categoryId())).toList().iterator();
        var ordered = new ArrayList<RecommendationProduct>();
        var used = new HashSet<Long>();
        var preferred = ranked.iterator();
        while (ordered.size() < limit) {
            var item = ordered.size() % 4 == 3 ? nextUnused(exploration, used) : null;
            if (item == null) item = nextUnused(preferred, used);
            if (item == null) break;
            ordered.add(item);
        }
        return ordered;
    }

    private RecommendationProduct nextUnused(Iterator<RecommendationProduct> candidates, Set<Long> used) {
        while (candidates.hasNext()) {
            var item = candidates.next();
            if (used.add(item.id())) return item;
        }
        return null;
    }

    record BrowserEvent(RecommendationEventType type, Long spuId, UUID requestId) {}
    record Feed(String requestId, boolean personalized, List<Card> items) {}
    private record IssuedRequest(long userId, Set<String> spuIds) {}
    private record DailySignal(long spuId, RecommendationEventType type, long day) {}
    private record Taste(Map<Long, Double> categories, Map<Long, Double> brands, Set<Long> seenProducts, double price,
                         double maxCategory, double maxBrand) {}
    record Card(String id, String name, String description, String mainImage, BigDecimal minPrice, BigDecimal maxPrice,
                Integer reviewCount) {
        static Card from(RecommendationProduct product) {
            return new Card(product.id().toString(), product.name(), product.description(), product.mainImage(),
                    product.minPrice(), product.maxPrice(), product.reviewCount());
        }
    }
}
