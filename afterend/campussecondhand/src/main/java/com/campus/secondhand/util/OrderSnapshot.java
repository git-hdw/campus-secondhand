package com.campus.secondhand.util;
import com.campus.secondhand.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
public final class OrderSnapshot {
    private static final ObjectMapper JSON = new ObjectMapper();
    private OrderSnapshot() {}
    public static String capture(Product product) {
        var data = JSON.createObjectNode();
        data.put("id", product.getId()); data.put("userId", product.getUserId());
        data.put("title", product.getTitle()); data.put("description", product.getDescription());
        data.put("images", product.getImages()); data.put("tradePlace", product.getTradePlace());
        data.put("category", product.getCategory()); data.put("condition", product.getCondition());
        data.put("price", product.getPrice()); data.put("originalPrice", product.getOriginalPrice());
        return data.toString();
    }
    public static JsonNode read(String value) {
        try { return value == null ? JSON.createObjectNode() : JSON.readTree(value); }
        catch (java.io.IOException e) { throw new IllegalStateException("Invalid stored order snapshot", e); }
    }
}
