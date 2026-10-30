package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.CategoryRepository;
import com.campus.secondhand.repository.OrderRepository;
import com.campus.secondhand.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.campus.secondhand.exception.TradeStateException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderTradeService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.campus.secondhand.repository.FavoriteRepository favoriteRepository;

    @Autowired
    private com.campus.secondhand.repository.MessageRepository messageRepository;

    @Transactional
    public ApiResponse<?> place(User buyer, Long productId) {
        Product product = productRepository.lockById(productId);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        if (product.getUserId() != null && product.getUserId().equals(buyer.getId())) {
            return ApiResponse.error(400, "不能购买自己的商品");
        }
        if (product.getStatus() == null || product.getStatus() != 0) {
            return ApiResponse.error(400, "商品当前不可购买");
        }
        if (completedCount(productId) > 0) {
            return ApiResponse.error(400, "商品已成交，不能再购买");
        }
        QueryWrapper<Order> pending = new QueryWrapper<>();
        pending.eq("product_id", productId).eq("status", 0);
        if (orderRepository.selectCount(pending) > 0) {
            return ApiResponse.error(400, "已有待见面订单");
        }
        Order order = new Order();
        order.setOrderNo("ORD" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 26));
        order.setProductId(productId);
        order.setBuyerId(buyer.getId());
        order.setSellerId(product.getUserId());
        order.setPrice(product.getPrice());
        order.setSnapshotJson(com.campus.secondhand.util.OrderSnapshot.capture(product));
        order.setStatus(0);
        order.setCreatedTime(LocalDateTime.now());
        orderRepository.insert(order);
        return ApiResponse.success("创建成功", order);
    }

    @Transactional
    public ApiResponse<?> finish(Long orderId, Long productId, int target) {
        Product product = productId == null ? null : productRepository.lockById(productId);
        if (product == null) {
            if (target == 4 && movePendingOrder(orderId, 4)) {
                return ApiResponse.success("取消成功", orderRepository.selectById(orderId));
            }
            return ApiResponse.error(404, "商品不存在");
        }
        long completed = completedCount(productId);
        if (target == 3) {
            if (completed > 0) {
                return ApiResponse.error(400, "商品已成交，不能再次完成");
            }
            if (product.getStatus() == null || product.getStatus() != 0) {
                return ApiResponse.error(400, "商品当前不可完成");
            }
            UpdateWrapper<Product> sold = new UpdateWrapper<>();
            sold.eq("id", productId).eq("status", 0)
                    .set("status", 1)
                    .set("sold_time", LocalDateTime.now())
                    .set("updated_time", LocalDateTime.now());
            if (productRepository.update(null, sold) != 1) {
                return ApiResponse.error(400, "商品当前不可完成");
            }
            if (!movePendingOrder(orderId, target)) {
                throw new TradeStateException("订单已结束，不能再变更");
            }
            return ApiResponse.success("确认成功", orderRepository.selectById(orderId));
        }
        if (!movePendingOrder(orderId, 4)) {
            return ApiResponse.error(400, "订单已结束，不能再变更");
        }
        if (completed == 0) {
            UpdateWrapper<Product> restore = new UpdateWrapper<>();
            restore.eq("id", productId).eq("status", 1)
                    .set("status", 0)
                    .set("updated_time", LocalDateTime.now());
            productRepository.update(null, restore);
        }
        return ApiResponse.success("取消成功", orderRepository.selectById(orderId));
    }

    private long completedCount(Long productId) {
        List<Long> ids = orderRepository.lockCompletedIds(productId);
        return ids == null ? 0 : ids.size();
    }

    private boolean movePendingOrder(Long orderId, int target) {
        UpdateWrapper<Order> orderUpdate = new UpdateWrapper<>();
        orderUpdate.eq("id", orderId).eq("status", 0)
                .set("status", target)
                .set("updated_time", LocalDateTime.now());
        if (target == 3) orderUpdate.set("completed_time", LocalDateTime.now());
        return orderRepository.update(null, orderUpdate) == 1;
    }

    @Transactional
    public ApiResponse<?> changeOrder(User actor, Long orderId, int target) {
        if (target != 3 && target != 4) {
            return ApiResponse.error(400, "只能确认完成或取消");
        }
        Order existing = orderRepository.selectById(orderId);
        if (existing == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        boolean buyer = actor.getId().equals(existing.getBuyerId());
        boolean seller = actor.getId().equals(existing.getSellerId());
        if (!buyer && !seller) {
            return ApiResponse.error(403, "无权操作此订单");
        }
        if (target == 3 && !buyer) {
            return ApiResponse.error(403, "只有买家可以确认完成");
        }
        return finish(existing.getId(), existing.getProductId(), target);
    }

    @Transactional
    public ApiResponse<?> changeProductStatus(Long productId, int target) {
        return changeProductStatuses(List.of(productId), target);
    }

    @Transactional
    public ApiResponse<?> changeProductStatuses(List<Long> ids, int target) {
        if (target != 0 && target != 2 && target != 3) {
            return ApiResponse.error(400, "商品状态无效");
        }
        List<Product> locked = new ArrayList<>();
        for (Long id : ids.stream().distinct().sorted().toList()) {
            Product product = productRepository.lockById(id);
            if (product == null) {
                return ApiResponse.error(404, "商品不存在");
            }
            ApiResponse<?> denied = denyProductStatus(product, target);
            if (denied != null) {
                return denied;
            }
            locked.add(product);
        }
        for (Product product : locked) {
            if (product.getStatus() != null && product.getStatus() == target) {
                continue;
            }
            product.setStatus(target);
            product.setUpdatedTime(LocalDateTime.now());
            productRepository.updateById(product);
        }
        return ApiResponse.success("更新成功", null);
    }

    private ApiResponse<?> denyProductStatus(Product product, int target) {
        QueryWrapper<Order> pending = new QueryWrapper<>();
        pending.eq("product_id", product.getId()).eq("status", 0);
        if (orderRepository.selectCount(pending) > 0 && (product.getStatus() == null || product.getStatus() != target)) {
            return ApiResponse.error(400, "有待见面订单，不能修改商品状态");
        }
        boolean sold = product.getStatus() != null && product.getStatus() == 1;
        if (!sold) {
            QueryWrapper<Order> done = new QueryWrapper<>();
            done.eq("product_id", product.getId()).eq("status", 3);
            sold = orderRepository.selectCount(done) > 0;
        }
        if (sold && (target == 0 || target == 3)) {
            return ApiResponse.error(400, "已成交商品不能重新上架");
        }
        return null;
    }

    @Transactional
    public ApiResponse<?> editProduct(User actor, Long productId, Map<String, Object> params) {
        Product product = productRepository.lockById(productId);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        boolean admin = actor.getRole() != null && actor.getRole() == 1;
        if (!actor.getId().equals(product.getUserId()) && !admin) {
            return ApiResponse.error(403, "无权操作");
        }
        if (!admin && product.getStatus() != null && product.getStatus() != 0 && product.getStatus() != 3) {
            return ApiResponse.error(400, "当前状态不能编辑");
        }
        QueryWrapper<Order> pending = new QueryWrapper<>();
        pending.eq("product_id", productId).eq("status", 0);
        if (orderRepository.selectCount(pending) > 0) {
            return ApiResponse.error(400, "有待见面订单，不能修改商品");
        }
        UpdateWrapper<Product> update = new UpdateWrapper<>();
        update.eq("id", productId);
        if (params.get("title") != null) {
            String title = params.get("title").toString().trim();
            if (title.length() < 2 || title.length() > 60) {
                return ApiResponse.error(400, "标题需要 2 到 60 个字");
            }
            update.set("title", title);
        }
        if (params.get("description") != null) {
            String description = params.get("description").toString().trim();
            if (description.isEmpty() || description.length() > 500) {
                return ApiResponse.error(400, "商品描述需要 1 到 500 个字");
            }
            update.set("description", description);
        }
        if (params.get("price") != null) {
            BigDecimal price = com.campus.secondhand.util.MoneyAmount.parse(params.get("price"), false);
            if (price.signum() <= 0) {
                return ApiResponse.error(400, "价格必须大于 0");
            }
            update.set("price", price);
        }
        if (params.containsKey("originalPrice")) {
            update.set("original_price", params.get("originalPrice") == null ? null : com.campus.secondhand.util.MoneyAmount.parse(params.get("originalPrice"), true));
        }
        if (params.get("category") != null) {
            String category = params.get("category").toString().trim();
            Category known = categoryRepository.lockByName(category);
            if (known == null || !Integer.valueOf(1).equals(known.getStatus())) {
                return ApiResponse.error(400, "请选择分类");
            }
            update.set("category", category);
        }
        if (params.get("condition") != null) {
            String condition = params.get("condition").toString().trim();
            if (!java.util.List.of("全新", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "有磨损").contains(condition)) {
                return ApiResponse.error(400, "请选择成色");
            }
            update.set("`condition`", condition);
        }
        if (params.get("tradePlace") != null) {
            String place = params.get("tradePlace").toString().trim();
            if (!java.util.Objects.equals(place, product.getTradePlace()) && !java.util.List.of("图书馆", "教学楼", "食堂", "宿舍楼下", "操场").contains(place)) {
                return ApiResponse.error(400, "请选择见面地点");
            }
            update.set("trade_place", place);
        }
        if (params.get("images") != null) {
            update.set("images", params.get("images").toString());
        }
        update.set("updated_time", LocalDateTime.now());
        productRepository.update(null, update);
        return ApiResponse.success("更新成功", productRepository.selectById(productId));
    }

    @Transactional
    public ApiResponse<?> deleteProduct(User actor, Long productId) {
        Product product = productRepository.lockById(productId);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        if (!actor.getId().equals(product.getUserId())) {
            return ApiResponse.error(403, "无权操作");
        }
        QueryWrapper<Order> orders = new QueryWrapper<>();
        orders.eq("product_id", productId);
        if (orderRepository.selectCount(orders) > 0) {
            return ApiResponse.error(400, "商品已有订单，不能删除");
        }
        // Retain conversation history; a referenced product can be taken off sale instead.
        if (messageRepository.selectCount(new QueryWrapper<com.campus.secondhand.entity.Message>().eq("product_id", productId)) > 0)
            return ApiResponse.error(409, "商品有关联消息，请下架以保留沟通记录");
        favoriteRepository.delete(new QueryWrapper<com.campus.secondhand.entity.Favorite>().eq("product_id", productId));
        productRepository.deleteById(productId);
        return ApiResponse.success("删除成功", null);
    }
}
