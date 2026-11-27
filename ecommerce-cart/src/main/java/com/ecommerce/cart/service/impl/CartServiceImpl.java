package com.ecommerce.cart.service.impl;

import com.ecommerce.cart.common.CartErrorCode;
import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.result.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final long CART_TTL_DAYS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CartServiceImpl(RedisTemplate<String, Object> redisTemplate, ProductClient productClient) {
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    @Override
    public void addItem(Long userId, CartItem item) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        if (item.getSkuId() == null || item.getSkuId() <= 0) {
            throw new BusinessException(CartErrorCode.PRODUCT_UNAVAILABLE);
        }
        if (missingProduct(item)) {
            applySnapshot(item, loadSnapshots(List.of(item.getSkuId())).get(item.getSkuId()));
            if (missingProduct(item)) throw new BusinessException(CartErrorCode.PRODUCT_UNAVAILABLE);
        }
        String key = cartKey(userId);
        String field = String.valueOf(item.getSkuId());

        // If SKU already in cart, increment quantity
        Object existing = redisTemplate.opsForHash().get(key, field);
        if (existing != null) {
            try {
                CartItem existingItem = objectMapper.readValue(existing.toString(), CartItem.class);
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                if (missingProduct(existingItem)) {
                    existingItem.setSpuId(item.getSpuId());
                    existingItem.setName(item.getName());
                    existingItem.setImage(item.getImage());
                    existingItem.setPrice(item.getPrice());
                }
                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(existingItem));
                redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
                return;
            } catch (JsonProcessingException ignored) {}
        }

        if (item.getChecked() == null) item.setChecked(true);
        try {
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
            redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart item", e);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long skuId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        String key = cartKey(userId);
        String field = String.valueOf(skuId);

        Object existing = redisTemplate.opsForHash().get(key, field);
        if (existing == null) {
            throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        if (quantity == 0) {
            redisTemplate.opsForHash().delete(key, field);
            return;
        }

        try {
            CartItem item = objectMapper.readValue(existing.toString(), CartItem.class);
            item.setQuantity(quantity);
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process cart item", e);
        }
    }

    @Override
    public void removeItem(Long userId, Long skuId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(skuId));
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    @Override
    public List<CartItem> getCart(Long userId) {
        List<CartItem> items = readCart(userId);
        List<Long> missing = items.stream().filter(item -> missingProduct(item) && item.getSkuId() != null)
                .map(CartItem::getSkuId).toList();
        if (!missing.isEmpty()) {
            Map<Long, SkuBatchVO> snapshots = loadSnapshots(missing);
            items.stream().filter(item -> missingProduct(item) && item.getSkuId() != null)
                    .forEach(item -> applySnapshot(item, snapshots.get(item.getSkuId())));
        }
        return items;
    }

    private List<CartItem> readCart(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId));
        List<CartItem> items = new ArrayList<>();
        for (Object value : entries.values()) {
            try {
                items.add(objectMapper.readValue(value.toString(), CartItem.class));
            } catch (JsonProcessingException ignored) {}
        }
        return items;
    }

    @Override
    public int getCount(Long userId) {
        return readCart(userId).stream().mapToInt(CartItem::getQuantity).sum();
    }

    private boolean missingProduct(CartItem item) {
        return item.getSpuId() == null || item.getPrice() == null || item.getName() == null || item.getName().isBlank();
    }

    private Map<Long, SkuBatchVO> loadSnapshots(List<Long> skuIds) {
        try {
            var result = productClient.getSkus(skuIds);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                Map<Long, SkuBatchVO> snapshots = new HashMap<>();
                for (SkuBatchVO sku : result.getData()) snapshots.put(sku.getSkuId(), sku);
                return snapshots;
            }
        } catch (RuntimeException ignored) {
            // Previously saved cart rows remain readable when the catalog is temporarily unavailable.
        }
        return Map.of();
    }

    private void applySnapshot(CartItem item, SkuBatchVO sku) {
        if (sku == null) return;
        item.setSpuId(sku.getSpuId());
        item.setName(sku.getSkuName());
        if (item.getImage() == null || item.getImage().isBlank()) item.setImage(sku.getImage());
        item.setPrice(sku.getPrice());
    }

    @Override
    public void toggleCheck(Long userId, Long skuId) {
        String key = cartKey(userId);
        String field = String.valueOf(skuId);
        Object existing = redisTemplate.opsForHash().get(key, field);
        if (existing == null) {
            throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }
        try {
            CartItem item = objectMapper.readValue(existing.toString(), CartItem.class);
            item.setChecked(!Boolean.TRUE.equals(item.getChecked()));
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process cart item", e);
        }
    }

    @Override
    public void mergeCart(Long userId, String anonymousToken) {
        String anonKey = CART_KEY_PREFIX + "anon:" + anonymousToken;
        String userKey = cartKey(userId);

        Map<Object, Object> anonEntries = redisTemplate.opsForHash().entries(anonKey);
        for (Map.Entry<Object, Object> entry : anonEntries.entrySet()) {
            String skuField = entry.getKey().toString();
            Object existing = redisTemplate.opsForHash().get(userKey, skuField);
            if (existing != null) {
                try {
                    CartItem anonItem = objectMapper.readValue(entry.getValue().toString(), CartItem.class);
                    CartItem userItem = objectMapper.readValue(existing.toString(), CartItem.class);
                    userItem.setQuantity(userItem.getQuantity() + anonItem.getQuantity());
                    redisTemplate.opsForHash().put(userKey, skuField, objectMapper.writeValueAsString(userItem));
                } catch (JsonProcessingException ignored) {}
            } else {
                redisTemplate.opsForHash().put(userKey, skuField, entry.getValue());
            }
        }
        // Delete anonymous cart after merge
        redisTemplate.delete(anonKey);
    }
}
