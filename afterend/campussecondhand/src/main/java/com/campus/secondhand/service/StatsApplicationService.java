package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class StatsApplicationService {

    @Autowired
    private JdbcTemplate jdbc;


    @Autowired
    private UserRepository userRepository;

    public ApiResponse<?> stats(
            String authHeader) {
        Long userId = currentUserId(authHeader);
        Map<String, Object> data = new HashMap<>();
        data.put("activeUsers", count("SELECT COUNT(*) FROM users WHERE status = 1 AND deleted = 0"));
        data.put("onSaleProducts", count("SELECT COUNT(*) FROM products WHERE status = 0"));
        data.put("categoryCounts", jdbc.queryForList(
                "SELECT category, COUNT(*) AS count FROM products WHERE status = 0 GROUP BY category"));
        data.put("completedOrders", count("SELECT COUNT(*) FROM orders WHERE status = 3"));
        if (userId == null) {
            data.put("pendingOrders", 0);
            data.put("unreadMessages", 0);
        } else {
            data.put("pendingOrders", count(
                    "SELECT COUNT(*) FROM orders WHERE status = 0 AND (buyer_id = ? OR seller_id = ?)",
                    userId, userId));
            data.put("unreadMessages", count(
                    "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = 0",
                    userId));
        }
        return (ApiResponse.success("获取成功", data));
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private Long currentUserId(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get().map(User::getId).orElse(null);
    }
}
