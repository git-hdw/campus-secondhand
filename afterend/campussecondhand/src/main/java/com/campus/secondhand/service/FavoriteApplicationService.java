package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.FavoriteRepository;
import com.campus.secondhand.repository.ProductRepository;
import com.campus.secondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.campus.secondhand.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class FavoriteApplicationService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.campus.secondhand.service.FavoriteService favoriteService;

    private Optional<User> getUserFromToken(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    public ApiResponse<?> getMyFavorites(String authHeader, Integer page, Integer pageSize) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userOpt.get().getId());
        var loaded = favoriteRepository.selectPage(new Page<Favorite>(PageResult.number(page), PageResult.size(pageSize)), wrapper.orderByDesc("id"));
        List<Favorite> favorites = loaded.getRecords();
        var products = com.campus.secondhand.util.BatchLookup.products(productRepository, favorites.stream().map(Favorite::getProductId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", favorite.getId());
            row.put("productId", favorite.getProductId());
            Product product = products.get(favorite.getProductId());
            row.put("title", product == null ? "商品已删除" : product.getTitle());
            row.put("price", product == null ? "" : product.getPrice());
            String image = "";
            if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                image = com.campus.secondhand.util.ImageList.first(product.getImages());
            }
            row.put("image", image);
            rows.add(row);
        }
        return (ApiResponse.success("获取成功", new PageResult<>(rows, loaded.getTotal(), loaded.getCurrent(), loaded.getSize())));
    }

    public ApiResponse<?> getFavoriteById(
            String authHeader,
            Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Favorite favorite = favoriteRepository.selectById(id);
        if (favorite == null) {
            return (ApiResponse.error(404, "收藏不存在"));
        }
        if (!favorite.getUserId().equals(userOpt.get().getId())) {
            return (ApiResponse.error(403, "无权访问此收藏"));
        }
        return (ApiResponse.success("获取成功", favorite));
    }

    public ApiResponse<?> createFavorite(
            String authHeader,
            Favorite favorite) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (favoriteService.create(userOpt.get(), favorite.getProductId()));
    }

    public ApiResponse<?> deleteFavorite(
            String authHeader,
            Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Favorite favorite = favoriteRepository.selectById(id);
        if (favorite == null) {
            return (ApiResponse.error(404, "收藏不存在"));
        }
        if (!favorite.getUserId().equals(userOpt.get().getId())) {
            return (ApiResponse.error(403, "无权操作此收藏"));
        }
        favoriteRepository.deleteById(id);
        return (ApiResponse.success("取消收藏成功", null));
    }

    public ApiResponse<?> checkFavorite(
            String authHeader,
            Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userOpt.get().getId()).eq("product_id", productId);
        Favorite favorite = favoriteRepository.selectOne(wrapper);
        return (ApiResponse.success("获取成功", favorite != null));
    }
}