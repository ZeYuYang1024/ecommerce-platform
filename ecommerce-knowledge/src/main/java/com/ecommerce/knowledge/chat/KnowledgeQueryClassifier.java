package com.ecommerce.knowledge.chat;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class KnowledgeQueryClassifier {

    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("(?i)\\b(?:ord-?\\d+|\\d{12,32})\\b");

    public KnowledgeQueryRoute classify(String question) {
        return classify(extractFeatures(question));
    }

    public KnowledgeQueryFeatures extractFeatures(String question) {
        if (StrUtil.isBlank(question)) {
            return new KnowledgeQueryFeatures("", false, false, false, false, false, false);
        }

        String normalized = question.trim().toLowerCase(Locale.ROOT);
        boolean userScoped = containsAny(normalized,
                "我的", "我想", "我要", "帮我", "查一下",
                "my ", " my", "show ", "list ", "check ", "view ", "current ", "latest ", "recent ");
        boolean hasOrderNo = ORDER_NO_PATTERN.matcher(question).find();
        boolean policyFaq = isPolicyFaq(normalized);
        boolean realtimeIntent = containsAny(normalized,
                "售后", "退货", "退款", "换货",
                "订单", "order", "orders",
                "购物车", "cart",
                "地址", "收货地址", "address", "shipping address",
                "通知", "站内信", "notification",
                "支付", "付款", "pay", "payment",
                "优惠券", "coupon");
        boolean productIntent = containsAny(normalized, "商品", "产品", "product");
        boolean inventoryIntent = containsAny(normalized, "库存", "inventory", "stock");
        return new KnowledgeQueryFeatures(normalized, userScoped, hasOrderNo, policyFaq, realtimeIntent, productIntent, inventoryIntent);
    }

    public KnowledgeQueryRoute classify(KnowledgeQueryFeatures features) {
        if (features == null || StrUtil.isBlank(features.normalizedQuestion())) {
            return KnowledgeQueryRoute.RAG_FAQ;
        }

        String normalized = features.normalizedQuestion();
        boolean userScoped = features.userScoped();
        boolean hasOrderNo = features.hasOrderNo();
        boolean policyFaq = features.policyFaq();
        boolean afterSaleIntent = containsAny(normalized,
                "售后", "退货", "退款", "换货", "refund", "return", "exchange", "after-sale", "after sale");
        boolean orderIntent = containsAny(normalized, "订单", "order", "orders");
        boolean couponIntent = containsAny(normalized, "优惠券", "coupon");
        boolean couponDiscoveryIntent = containsAny(normalized,
                "可以领", "可领取", "可领", "领取", "available", "claim");

        if (containsAny(normalized, "支付", "付款", "pay", "payment") && hasOrderNo) {
            return KnowledgeQueryRoute.PAYMENT_BY_ORDER_NO;
        }
        if (afterSaleIntent && !policyFaq) {
            return KnowledgeQueryRoute.AFTER_SALE;
        }
        if (orderIntent && policyFaq) {
            return KnowledgeQueryRoute.RAG_FAQ;
        }
        if (orderIntent && (userScoped || hasOrderNo || containsAny(normalized, "status", "history", "list"))) {
            return KnowledgeQueryRoute.ORDER_LIST;
        }
        if (containsAny(normalized, "购物车", "cart") && userScoped) {
            return KnowledgeQueryRoute.CART;
        }
        if (containsAny(normalized, "地址", "收货地址", "address", "shipping address") && userScoped) {
            return KnowledgeQueryRoute.ADDRESS;
        }
        if (containsAny(normalized, "通知", "站内信", "notification") && userScoped) {
            return KnowledgeQueryRoute.NOTIFICATION;
        }
        if (couponIntent && !policyFaq && !couponDiscoveryIntent && userScoped) {
            return KnowledgeQueryRoute.COUPON;
        }
        if (features.inventoryIntent()) {
            return KnowledgeQueryRoute.INVENTORY;
        }
        if (features.productIntent()) {
            return KnowledgeQueryRoute.PRODUCT;
        }
        return KnowledgeQueryRoute.RAG_FAQ;
    }

    private boolean isPolicyFaq(String text) {
        return containsAny(text,
                "规则", "政策", "说明", "规定", "条件", "须知", "时效", "范围",
                "policy", "rule", "rules");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
