package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Message;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.repository.ProductRepository;
import com.campus.secondhand.repository.OrderRepository;
import com.campus.secondhand.repository.MessageRepository;
import com.campus.secondhand.repository.CategoryRepository;
import com.campus.secondhand.service.OrderTradeService;
import com.campus.secondhand.service.ProductService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;



@org.springframework.stereotype.Service
public class AdminApplicationService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CategoryRepository categoryRepository;


    @Autowired
    private OrderTradeService orderTradeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private com.campus.secondhand.service.CategoryService categoryService;

    private Optional<User> currentUser(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    private boolean isAdmin(String authHeader) {
        return currentUser(authHeader)
                .map(user -> user.getRole() != null && user.getRole() == 1)
                .orElse(false);
    }

    // ==================== 用户管理 ====================

    // 获取所有用户

    public ApiResponse<?> getAllUsers(
            String authHeader,
            String search,
            Integer page,
            Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            String keyword = search.trim();
            wrapper.and(w -> w.like("username", keyword).or().like("email", keyword));
        }
        wrapper.orderByDesc("created_time");
        Page<User> loaded = userRepository.selectPage(new Page<>(pageNo(page), pageLimit(pageSize)), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("items", loaded.getRecords());
        result.put("total", loaded.getTotal());
        result.put("page", loaded.getCurrent());
        result.put("pageSize", loaded.getSize());

        return (ApiResponse.success("获取成功", result));
    }

    // 获取用户详情

    public ApiResponse<?> getUserDetail(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        User user = userRepository.selectById(id);
        if (user == null) {
            return (ApiResponse.error(404, "用户不存在"));
        }

        return (ApiResponse.success("获取成功", user));
    }

    // 禁用/启用用户

    public ApiResponse<?> updateUserStatus(
            String authHeader,
            Long id,
            Integer status) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        User user = userRepository.selectById(id);
        if (user == null) {
            return (ApiResponse.error(404, "用户不存在"));
        }
        if (status == null || (status != 0 && status != 1)) {
            return (ApiResponse.error(400, "账号状态无效"));
        }
        if (currentUser(authHeader).map(actor -> actor.getId().equals(id)).orElse(false) && status == 0) {
            return (ApiResponse.error(400, "不能禁用当前登录账号"));
        }
        user.setStatus(status);
        userRepository.updateById(user);

        return (ApiResponse.success("更新成功", user));
    }

    // ==================== 商品管理 ====================

    // 获取所有商品（支持搜索、筛选、分页）

    public ApiResponse<?> getAllProducts(
            String authHeader,
            String search,
            String category,
            Integer status,
            Integer page,
            Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            String keyword = search.trim();
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("created_time");
        Page<Product> loaded = productRepository.selectPage(new Page<>(pageNo(page), pageLimit(pageSize)), wrapper);
        List<Product> paginatedProducts = loaded.getRecords();
        var sellers = com.campus.secondhand.util.BatchLookup.users(userRepository, paginatedProducts.stream().map(Product::getUserId).toList());
        for (Product product : paginatedProducts) {
            User seller = sellers.get(product.getUserId());
            if (seller != null) product.setSellerName(seller.getUsername());
        }
        productService.fillPending(paginatedProducts);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", paginatedProducts);
        result.put("total", loaded.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return (ApiResponse.success("获取成功", result));
    }

    // 获取商品详情

    public ApiResponse<?> getProductDetail(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        Product product = productRepository.selectById(id);
        if (product == null) {
            return (ApiResponse.error(404, "商品不存在"));
        }

        return (ApiResponse.success("获取成功", product));
    }

    // 更新商品状态

    public ApiResponse<?> updateProductStatus(
            String authHeader,
            Long id,
            Integer status) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        if (status == null) {
            return (ApiResponse.error(400, "商品状态无效"));
        }
        return (orderTradeService.changeProductStatus(id, status));
    }

    // 批量更新商品状态

    public ApiResponse<?> batchUpdateProductStatus(
            String authHeader,
            Map<String, Object> params) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        List<Long> ids = new ArrayList<>();
        Object rawIds = params.get("ids");
        if (rawIds instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    ids.add(com.campus.secondhand.util.RequestValue.positiveId(number));
                }
            }
        }
        Object rawStatus = params.get("status");
        if (ids.isEmpty() || ids.size() > 100 || !(rawStatus instanceof Number number)) {
            return (ApiResponse.error(400, "参数错误"));
        }
        return (orderTradeService.changeProductStatuses(ids, new java.math.BigDecimal(number.toString()).intValueExact()));
    }

    // ==================== 订单管理 ====================

    // 获取所有订单

    public ApiResponse<?> getAllOrders(
            String authHeader,
            String search,
            Integer status,
            Integer page,
            Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (search != null && !search.isBlank()) {
            wrapper.like("order_no", search.trim());
        }

        wrapper.orderByDesc("created_time");
        Page<Order> loaded = orderRepository.selectPage(new Page<>(pageNo(page), pageLimit(pageSize)), wrapper);
        var products = com.campus.secondhand.util.BatchLookup.products(productRepository, loaded.getRecords().stream().map(Order::getProductId).toList());
        var people = com.campus.secondhand.util.BatchLookup.users(userRepository, loaded.getRecords().stream().flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId())).toList());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Order order : loaded.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("status", order.getStatus());
            row.put("price", order.getPrice());
            row.put("createdTime", order.getCreatedTime());
            Product product = products.get(order.getProductId());
            row.put("productTitle", product != null ? product.getTitle() : "商品已删除");
            User buyer = people.get(order.getBuyerId());
            User seller = people.get(order.getSellerId());
            row.put("buyerName", buyer != null ? buyer.getUsername() : "未知买家");
            row.put("sellerName", seller != null ? seller.getUsername() : "未知卖家");
            items.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", loaded.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return (ApiResponse.success("获取成功", result));
    }

    // 获取订单详情

    public ApiResponse<?> getOrderDetail(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        Order order = orderRepository.selectById(id);
        if (order == null) {
            return (ApiResponse.error(404, "订单不存在"));
        }

        return (ApiResponse.success("获取成功", order));
    }

    // 更新订单状态

    public ApiResponse<?> updateOrderStatus(
            String authHeader,
            Long id,
            Map<String, String> params) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        String statusStr = params.get("status");
        if (statusStr == null || statusStr.isEmpty()) {
            return (ApiResponse.error(400, "状态不能为空"));
        }

        Order order = orderRepository.selectById(id);
        if (order == null) {
            return (ApiResponse.error(404, "订单不存在"));
        }
        int status;
        if ("COMPLETED".equalsIgnoreCase(statusStr)) {
            status = 3;
        } else if ("CANCELLED".equalsIgnoreCase(statusStr)) {
            status = 4;
        } else {
            try {
                status = Integer.parseInt(statusStr);
            } catch (NumberFormatException e) {
                return (ApiResponse.error(400, "状态值必须是数字"));
            }
        }
        Optional<User> actor = currentUser(authHeader);
        if (actor.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (orderTradeService.changeOrder(actor.get(), id, status));
    }

    // 删除订单

    public ApiResponse<?> deleteOrder(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        Order order = orderRepository.selectById(id);
        if (order == null) {
            return (ApiResponse.error(404, "订单不存在"));
        }
        if (order.getStatus() != null && (order.getStatus() == 0 || order.getStatus() == 3)) {
            return (ApiResponse.error(400, "进行中或已完成的订单不能删除"));
        }

        orderRepository.deleteById(id);
        return (ApiResponse.success("删除成功", null));
    }

    // ==================== 消息管理 ====================

    // 获取所有消息

    public ApiResponse<?> getAllMessages(
            String authHeader,
            String search,
            Integer page,
            Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.like("content", search);
        }

        wrapper.orderByDesc("created_time");
        Page<Message> loaded = messageRepository.selectPage(new Page<>(pageNo(page), pageLimit(pageSize)), wrapper);
        var people = com.campus.secondhand.util.BatchLookup.users(userRepository, loaded.getRecords().stream().flatMap(m -> java.util.stream.Stream.of(m.getSenderId(), m.getReceiverId())).toList());
        for (Message message : loaded.getRecords()) {
            if (message.getSenderId() != null) {
                message.setSender(people.get(message.getSenderId()));
            }
            if (message.getReceiverId() != null) {
                message.setReceiver(people.get(message.getReceiverId()));
            }
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", loaded.getRecords());
        result.put("total", loaded.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return (ApiResponse.success("获取成功", result));
    }

    // 删除消息

    public ApiResponse<?> deleteMessage(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        Message message = messageRepository.selectById(id);
        if (message == null) {
            return (ApiResponse.error(404, "消息不存在"));
        }

        messageRepository.deleteById(id);
        return (ApiResponse.success("删除成功", null));
    }

    // ==================== 分类管理 ====================

    // 获取所有分类

    public ApiResponse<?> getAllCategories(
            String authHeader) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        List<Category> categories = categoryRepository.selectList(null);
        categories.sort((a, b) -> Integer.compare(
                a.getSortOrder() == null ? 0 : a.getSortOrder(),
                b.getSortOrder() == null ? 0 : b.getSortOrder()));
        
        var counts = new java.util.HashMap<String, Map<String, Object>>();
        jdbc.queryForList("SELECT category,COUNT(*) AS total,SUM(status=0) AS onSale FROM products GROUP BY category")
            .forEach(row -> counts.put((String) row.get("category"), row));
        List<Map<String, Object>> categoriesWithCount = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", category.getId());
            categoryMap.put("name", category.getName());
            categoryMap.put("description", category.getDescription());
            categoryMap.put("sortOrder", category.getSortOrder());
            
            var count = counts.getOrDefault(category.getName(), Map.of("total", 0, "onSale", 0));
            categoryMap.put("productCount", count.get("total"));
            categoryMap.put("onSaleCount", count.get("onSale"));

            categoriesWithCount.add(categoryMap);
        }
        
        return (ApiResponse.success("获取成功", categoriesWithCount));
    }

    // 添加分类

    public ApiResponse<?> addCategory(
            String authHeader,
            Map<String, String> params) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        String name = params.get("name") == null ? "" : params.get("name").trim();
        String label = params.get("description") == null ? "" : params.get("description").trim();
        if (!name.matches("[a-z0-9]{2,20}")) {
            return (ApiResponse.error(400, "分类编码需要 2 到 20 位小写字母或数字"));
        }
        if (label.isEmpty() || label.length() > 20) {
            return (ApiResponse.error(400, "请填写 1 到 20 个字的分类名称"));
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category> exists = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        exists.eq("name", name);
        if (categoryRepository.selectCount(exists) > 0) {
            return (ApiResponse.error(400, "分类编码已存在"));
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(label);
        category.setStatus(1);
        category.setSortOrder(99);
        categoryRepository.insert(category);

        return (ApiResponse.success("添加成功", category));
    }

    // 更新分类

    public ApiResponse<?> updateCategory(
            String authHeader,
            Long id,
            Map<String, String> params) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        String label = params.get("description") == null ? "" : params.get("description").trim();
        if (label.isEmpty() || label.length() > 20) {
            return (ApiResponse.error(400, "请填写 1 到 20 个字的分类名称"));
        }

        Category category = categoryRepository.selectById(id);
        if (category == null) {
            return (ApiResponse.error(404, "分类不存在"));
        }

        category.setDescription(label);
        categoryRepository.updateById(category);

        return (ApiResponse.success("更新成功", category));
    }

    // 删除分类

    public ApiResponse<?> deleteCategory(
            String authHeader,
            Long id) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }
        return (categoryService.delete(id));
    }

    // ==================== 数据统计 ====================

    // 获取平台统计数据

    public ApiResponse<?> getStats(
            String authHeader) {
        if (!isAdmin(authHeader)) {
            return (ApiResponse.error(403, "权限不足"));
        }

        long userCount = userRepository.selectCount(null);
        long productCount = productRepository.selectCount(null);
        long orderCount = orderRepository.selectCount(null);
        long messageCount = messageRepository.selectCount(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("productCount", productCount);
        stats.put("orderCount", orderCount);
        stats.put("messageCount", messageCount);

        return (ApiResponse.success("获取成功", stats));
    }

    private static long pageNo(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static long pageLimit(Integer pageSize) {
        int size = pageSize == null ? 10 : pageSize;
        if (size < 1) return 10;
        return Math.min(size, 50);
    }
}

