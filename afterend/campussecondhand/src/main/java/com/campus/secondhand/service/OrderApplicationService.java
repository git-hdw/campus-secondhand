package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.OrderRepository;
import com.campus.secondhand.repository.ProductRepository;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.service.OrderTradeService;
import com.campus.secondhand.util.ImageList;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.campus.secondhand.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class OrderApplicationService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderTradeService orderTradeService;


    private Optional<User> getUserFromToken(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    public ApiResponse<?> getMyOrders(String authHeader, Integer page, Integer pageSize, Integer status, Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Long userId = userOpt.get().getId();
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("buyer_id", userId).or().eq("seller_id", userId));
        if (status != null) wrapper.eq("status", status);
        if (productId != null) wrapper.eq("product_id", productId);
        var loaded = orderRepository.selectPage(new Page<Order>(PageResult.number(page), PageResult.size(pageSize)), wrapper.orderByDesc("created_time", "id"));
        List<Order> orders = loaded.getRecords();
        orders.sort((a, b) -> {
            if (a.getCreatedTime() == null) return b.getCreatedTime() == null ? 0 : 1;
            if (b.getCreatedTime() == null) return -1;
            return b.getCreatedTime().compareTo(a.getCreatedTime());
        });

        var products = com.campus.secondhand.util.BatchLookup.products(productRepository, orders.stream().map(Order::getProductId).toList());
        var people = com.campus.secondhand.util.BatchLookup.users(userRepository, orders.stream().flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId())).toList());
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("productId", order.getProductId());
            orderMap.put("legacyProductId", order.getLegacyProductId());
            
            // 获取商品信息
            Product product = products.get(order.getProductId());
            if (product != null) {
                orderMap.put("productTitle", product.getTitle());
                orderMap.put("productImage", ImageList.first(product.getImages()));
            } else {
                orderMap.put("productTitle", "商品已删除");
                orderMap.put("productImage", "");
            }
            
            var snapshot = com.campus.secondhand.util.OrderSnapshot.read(order.getSnapshotJson());
            if (snapshot.hasNonNull("title")) {
                orderMap.put("productTitle", snapshot.path("title").asText());
                orderMap.put("productImage", ImageList.first(snapshot.path("images").asText("")));
            }
            orderMap.put("price", order.getPrice().toString());
            orderMap.put("status", order.getStatus());
            orderMap.put("createdAt", order.getCreatedTime() == null ? null : order.getCreatedTime().toString());
            orderMap.put("sellerId", order.getSellerId());
            orderMap.put("buyerId", order.getBuyerId());
            
            // 获取卖家信息
            User seller = people.get(order.getSellerId());
            User buyer = people.get(order.getBuyerId());
            orderMap.put("sellerName", seller != null ? seller.getUsername() : "未知卖家");
            orderMap.put("buyerName", buyer != null ? buyer.getUsername() : "未知买家");
            
            orderList.add(orderMap);
        }
        
        return (ApiResponse.success("获取成功", new PageResult<>(orderList, loaded.getTotal(), loaded.getCurrent(), loaded.getSize())));
    }

    public ApiResponse<?> getMyBuyerOrders(String authHeader, Integer page, Integer pageSize) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("buyer_id", userOpt.get().getId());
        var loaded = orderRepository.selectPage(new Page<Order>(PageResult.number(page), PageResult.size(pageSize)), wrapper.orderByDesc("id"));
        return ApiResponse.success("获取成功", new PageResult<>(loaded.getRecords(), loaded.getTotal(), loaded.getCurrent(), loaded.getSize()));
    }

    public ApiResponse<?> getMySellerOrders(String authHeader, Integer page, Integer pageSize) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("seller_id", userOpt.get().getId());
        var loaded = orderRepository.selectPage(new Page<Order>(PageResult.number(page), PageResult.size(pageSize)), wrapper.orderByDesc("id"));
        return ApiResponse.success("获取成功", new PageResult<>(loaded.getRecords(), loaded.getTotal(), loaded.getCurrent(), loaded.getSize()));
    }

    public ApiResponse<?> getOrderById(
            String authHeader,
            Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return (ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return (ApiResponse.error(403, "无权访问此订单"));
        }
        return (ApiResponse.success("获取成功", order));
    }

    public ApiResponse<?> createOrder(
            String authHeader,
            Map<String, Long> request) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        Long productId = request.get("productId");
        if (productId == null) {
            return (ApiResponse.error(400, "商品ID不能为空"));
        }
        
        return (orderTradeService.place(userOpt.get(), productId));
    }

    public ApiResponse<?> confirmOrder(
            String authHeader,
            Long id) {
        return changeStatus(authHeader, id, 3);
    }

    public ApiResponse<?> cancelOrder(
            String authHeader,
            Long id) {
        return changeStatus(authHeader, id, 4);
    }

    public ApiResponse<?> updateOrder(
            String authHeader,
            Long id,
            Order order) {
        if (order.getStatus() == null) {
            return (ApiResponse.error(400, "请指定订单状态"));
        }
        return changeStatus(authHeader, id, order.getStatus());
    }

    private ApiResponse<?> changeStatus(String authHeader, Long id, int target) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (orderTradeService.changeOrder(userOpt.get(), id, target));
    }

    public ApiResponse<?> deleteOrder(
            String authHeader,
            Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return (ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return (ApiResponse.error(403, "无权操作此订单"));
        }
        if (order.getStatus() != null && (order.getStatus() == 0 || order.getStatus() == 3)) {
            return (ApiResponse.error(400, "进行中或已完成的订单不能删除"));
        }
        orderRepository.deleteById(id);
        return (ApiResponse.success("删除成功", null));
    }
}
