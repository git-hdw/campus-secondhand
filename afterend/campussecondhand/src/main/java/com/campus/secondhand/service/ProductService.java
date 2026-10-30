package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.repository.OrderRepository;
import com.campus.secondhand.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.campus.secondhand.repository.CategoryRepository categoryRepository;

    public Page<Product> pageOnSale(long page, long size, String category, String keyword,
                                    String condition, BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0);
        if (category != null && !category.isBlank() && !"all".equals(category)) {
            wrapper.eq("category", category.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = likePattern(keyword);
            wrapper.and(w -> w.apply("title LIKE {0} OR description LIKE {0}", pattern));
        }
        if (condition != null && !condition.isBlank()) {
            wrapper.eq("`condition`", condition.trim());
        }
        if (minPrice != null) {
            wrapper.ge("price", minPrice);
        }
        if (maxPrice != null) {
            wrapper.le("price", maxPrice);
        }
        if ("price-low".equals(sort)) {
            wrapper.orderByAsc("price").orderByDesc("id");
        } else if ("price-high".equals(sort)) {
            wrapper.orderByDesc("price").orderByDesc("id");
        } else if ("views".equals(sort)) {
            wrapper.orderByDesc("view_count").orderByDesc("id");
        } else {
            wrapper.orderByDesc("created_time").orderByDesc("id");
        }
        long limit = size < 1 ? 12 : Math.min(size, 50);
        long current = page < 1 ? 1 : page;
        return productRepository.selectPage(new Page<>(current, limit), wrapper);
    }

    private static String likePattern(String keyword) {
        String word = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + word + "%";
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    public java.util.Map<String,Object> personalPage(Long userId, Integer page, Integer size) {
        var loaded = productRepository.selectPage(new Page<Product>(com.campus.secondhand.common.PageResult.number(page), com.campus.secondhand.common.PageResult.size(size)), new QueryWrapper<Product>().eq("user_id", userId).orderByDesc("created_time", "id"));
        fillPending(loaded.getRecords());
        var stats = jdbc.queryForMap("SELECT COALESCE(SUM(status=0),0) AS listings,COALESCE(SUM(status=1),0) AS sold,COALESCE(SUM(view_count),0) AS views FROM products WHERE user_id=?", userId);
        return java.util.Map.of("items", loaded.getRecords(), "total", loaded.getTotal(), "page", loaded.getCurrent(), "pageSize", loaded.getSize(), "stats", stats);
    }

    public Product findById(Long id) {
        return productRepository.selectById(id);
    }

    public Product findByIdWithViewCount(Long id) {
        productRepository.increaseViewCount(id);
        return productRepository.selectById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public Product create(Product product) {
        var category = categoryRepository.lockByName(product.getCategory());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) throw new IllegalArgumentException("请选择有效分类");
        product.setCreatedTime(LocalDateTime.now());
        product.setUpdatedTime(LocalDateTime.now());
        product.setViewCount(0);
        product.setStatus(3);
        productRepository.insert(product);
        return product;
    }

    public boolean hasPendingOrder(Long productId) {
        QueryWrapper<Order> pending = new QueryWrapper<>();
        pending.eq("product_id", productId).eq("status", 0);
        return orderRepository.selectCount(pending) > 0;
    }

    public void fillPending(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        List<Long> ids = new java.util.ArrayList<>();
        for (Product product : products) {
            if (product.getId() != null) ids.add(product.getId());
        }
        if (ids.isEmpty()) return;
        QueryWrapper<Order> pending = new QueryWrapper<>();
        pending.select("product_id").eq("status", 0).in("product_id", ids);
        Set<Long> locked = new HashSet<>();
        for (Order row : orderRepository.selectList(pending)) {
            if (row.getProductId() != null) locked.add(row.getProductId());
        }
        for (Product product : products) {
            product.setPendingOrder(locked.contains(product.getId()));
        }
    }

}
