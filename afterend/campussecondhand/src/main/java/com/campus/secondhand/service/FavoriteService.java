package com.campus.secondhand.service;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.*;
import com.campus.secondhand.repository.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FavoriteService {
    private final ProductRepository products;
    private final FavoriteRepository favorites;
    public FavoriteService(ProductRepository products, FavoriteRepository favorites) { this.products = products; this.favorites = favorites; }
    @Transactional
    public ApiResponse<?> create(User actor, Long productId) {
        if (productId == null || productId <= 0) return ApiResponse.error(400, "商品 ID 无效");
        Product product = products.lockById(productId);
        if (product == null || !Integer.valueOf(0).equals(product.getStatus())) return ApiResponse.error(404, "商品不存在或已下架");
        Favorite existing = favorites.selectOne(new QueryWrapper<Favorite>().eq("user_id", actor.getId()).eq("product_id", productId));
        if (existing != null) return ApiResponse.success("已经收藏", existing);
        Favorite saved = new Favorite(); saved.setUserId(actor.getId()); saved.setProductId(productId);
        saved.setCreatedTime(java.time.LocalDateTime.now()); favorites.insert(saved);
        return ApiResponse.success("收藏成功", saved);
    }
}
