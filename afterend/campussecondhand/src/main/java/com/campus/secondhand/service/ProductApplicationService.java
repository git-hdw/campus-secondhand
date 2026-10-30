package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.repository.CategoryRepository;
import com.campus.secondhand.repository.UserRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.service.OrderTradeService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.ImageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class ProductApplicationService {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderTradeService orderTradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.campus.secondhand.service.IdempotencyService idempotency;

    @Autowired
    private CategoryRepository categoryRepository;


    private Optional<User> currentUser(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    private String imageError(String images) {
        try { com.campus.secondhand.util.ImageValue.validateList(images); return null; }
        catch (IllegalArgumentException e) { return e.getMessage(); }
    }

    public ApiResponse<?> list(
            Integer page,
            Integer pageSize,
            String category,
            String keyword,
            String condition,
            String minPrice,
            String maxPrice,
            String sort) {
        return onSalePage(page, pageSize, category, keyword, condition, minPrice, maxPrice, sort);
    }

    public ApiResponse<?> myProducts(String authHeader, Integer page, Integer pageSize) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }
            return ApiResponse.success("获取成功", productService.personalPage(userOpt.get().getId(), page, pageSize));
        } catch (Exception e) {
            throw new IllegalStateException("商品查询失败", e);
        }
    }

    public ApiResponse<?> byCategory(
            String category,
            Integer page,
            Integer pageSize) {
        return onSalePage(page, pageSize, category, null, null, null, null, "newest");
    }

    public ApiResponse<?> search(
            String keyword,
            Integer page,
            Integer pageSize) {
        return onSalePage(page, pageSize, null, keyword, null, null, null, "newest");
    }

    private ApiResponse<?> onSalePage(
            Integer page, Integer pageSize, String category, String keyword,
            String condition, String minPrice, String maxPrice, String sort) {
        if (condition != null && !condition.isBlank() && validCondition(condition) == null) {
            return (ApiResponse.error(400, "请选择成色"));
        }
        BigDecimal min;
        BigDecimal max;
        try {
            min = money(minPrice);
            max = money(maxPrice);
        } catch (IllegalArgumentException e) {
            return (ApiResponse.error(400, "价格格式不正确"));
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            return (ApiResponse.error(400, "最低价格不能大于最高价格"));
        }
        Page<Product> loaded = productService.pageOnSale(
                page == null ? 1 : page,
                pageSize == null ? 12 : pageSize,
                category, keyword, condition, min, max, sort);
        productService.fillPending(loaded.getRecords());
        Map<String, Object> result = new HashMap<>();
        result.put("items", loaded.getRecords());
        result.put("total", loaded.getTotal());
        result.put("page", loaded.getCurrent());
        result.put("pageSize", loaded.getSize());
        return (ApiResponse.success("获取成功", result));
    }

    private static BigDecimal money(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return com.campus.secondhand.util.MoneyAmount.parse(raw.trim(), true);
    }

    public ApiResponse<?> detail(
            Long id,
            String authHeader) {
        Product product = productService.findById(id);
        if (product == null) {
            return (ApiResponse.error(404, "商品不存在"));
        }
        boolean onSale = product.getStatus() != null && product.getStatus() == 0;
        if (!onSale) {
            Optional<User> viewer = currentUser(authHeader);
            boolean owner = viewer.isPresent() && viewer.get().getId().equals(product.getUserId());
            boolean admin = viewer.isPresent() && viewer.get().getRole() != null && viewer.get().getRole() == 1;
            if (!owner && !admin) {
                return (ApiResponse.error(404, "商品不存在"));
            }
        } else {
            product = productService.findByIdWithViewCount(id);
        }
        if (product != null) {
            User seller = userRepository.selectById(product.getUserId());
            if (seller != null) {
                product.setSeller(seller);
                product.setSellerName(seller.getUsername());
            }
            product.setPendingOrder(productService.hasPendingOrder(id));
            return (ApiResponse.success("获取成功", product));
        }
        return (ApiResponse.error(404, "商品不存在"));
    }

    public ApiResponse<?> create(
            String authHeader,
            String requestKey,
            Map<String, Object> params) {
        try {
            // 从 token 中获取当前登录用户
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }
            Long userId = userOpt.get().getId();

            Product product = new Product();
            product.setUserId(userId);
            product.setTitle(com.campus.secondhand.util.RequestValue.text(params.get("title")));
            product.setDescription(com.campus.secondhand.util.RequestValue.text(params.get("description")));
            product.setPrice(com.campus.secondhand.util.MoneyAmount.parse(params.get("price"), false));
            if (params.get("originalPrice") != null) {
                product.setOriginalPrice(com.campus.secondhand.util.MoneyAmount.parse(params.get("originalPrice"), true));
            }
            String category = params.get("category") == null ? "" : params.get("category").toString().trim();
            QueryWrapper<Category> known = new QueryWrapper<>();
            known.eq("name", category).eq("status", 1);
            if (categoryRepository.selectCount(known) == 0) {
                return (ApiResponse.error(400, "请选择分类"));
            }
            product.setCategory(category);
            String condition = validCondition(params.get("condition"));
            if (condition == null) {
                return (ApiResponse.error(400, "请选择成色"));
            }
            product.setCondition(condition);
            String tradePlace = validTradePlace(params.get("tradePlace"));
            if (tradePlace == null) {
                return (ApiResponse.error(400, "请选择见面地点"));
            }
            product.setTradePlace(tradePlace);
            String description = product.getDescription() == null ? "" : product.getDescription().trim();
            if (description.isEmpty() || description.length() > 500) {
                return (ApiResponse.error(400, "商品描述需要 1 到 500 个字"));
            }
            product.setDescription(description);
            String title = product.getTitle() == null ? "" : product.getTitle().trim();
            if (title.length() < 2 || title.length() > 60) {
                return (ApiResponse.error(400, "标题需要 2 到 60 个字"));
            }
            if (product.getPrice() == null || product.getPrice().signum() <= 0) {
                return (ApiResponse.error(400, "价格必须大于 0"));
            }
            String images = params.get("images") == null ? "" : params.get("images").toString();
            String imageError = imageError(images);
            if (imageError != null) {
                return (ApiResponse.error(400, imageError));
            }
            product.setTitle(title);
            product.setImages(images);

            return (idempotency.execute(userId, "product", requestKey, params, () -> ApiResponse.success("发布成功", productService.create(product))));
        } catch (IllegalArgumentException e) {
            return (ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return (ApiResponse.error(500, "发布失败，请稍后重试"));
        }
    }

    private String validCondition(Object raw) {
        if (raw == null) return null;
        String condition = raw.toString().trim();
        if (!java.util.List.of("全新", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "有磨损").contains(condition)) return null;
        return condition;
    }

    private String validTradePlace(Object raw) {
        if (raw == null) return null;
        String place = raw.toString().trim();
        if (!java.util.List.of("图书馆", "教学楼", "食堂", "宿舍楼下", "操场").contains(place)) return null;
        return place;
    }

    public ApiResponse<?> offShelf(
            Long id,
            String authHeader) {
        Optional<User> userOpt = currentUser(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Product product = productService.findById(id);
        if (product == null) {
            return (ApiResponse.error(404, "商品不存在"));
        }
        if (!product.getUserId().equals(userOpt.get().getId())) {
            return (ApiResponse.error(403, "无权操作"));
        }
        if (product.getStatus() == null || product.getStatus() != 0) {
            return (ApiResponse.error(400, "只有在售商品可以下架"));
        }
        return (orderTradeService.changeProductStatus(id, 2));
    }

    public ApiResponse<?> update(
            Long id,
            String authHeader,
            Map<String, Object> params) {
        try {
            Optional<User> userOpt = currentUser(authHeader);
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "未登录或登录已过期"));
            }
            if (params.get("images") != null) {
                String imageError = imageError(params.get("images").toString());
                if (imageError != null) {
                    return (ApiResponse.error(400, imageError));
                }
            }
            return (orderTradeService.editProduct(userOpt.get(), id, params));
        } catch (IllegalArgumentException e) {
            return (ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return (ApiResponse.error(500, "更新失败，请稍后重试"));
        }
    }

    public ApiResponse<?> delete(
            Long id,
            String authHeader) {
        Optional<User> userOpt = currentUser(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (orderTradeService.deleteProduct(userOpt.get(), id));
    }

    public ApiResponse<?> markAsSold(
            Long id,
            String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }

            Product product = productService.findById(id);
            if (product != null && !product.getUserId().equals(userOpt.get().getId())) {
                return (ApiResponse.error(403, "无权操作"));
            }
            return (ApiResponse.error(400, "只能由买家确认完成后成交"));
        } catch (Exception e) {
            return (ApiResponse.error(500, "操作失败"));
        }
    }
}
