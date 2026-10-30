package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    @Autowired
    private com.campus.secondhand.service.OrderApplicationService application;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyOrders(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) Integer status, @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(application.getMyOrders(authHeader, page, pageSize, status, productId));
    }

    @GetMapping("/my/buyer")
    public ResponseEntity<ApiResponse<?>> getMyBuyerOrders(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(application.getMyBuyerOrders(authHeader, page, pageSize));
    }

    @GetMapping("/my/seller")
    public ResponseEntity<ApiResponse<?>> getMySellerOrders(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(application.getMySellerOrders(authHeader, page, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getOrderById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.getOrderById(authHeader, id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> request) {
        return ResponseEntity.ok(application.createOrder(authHeader, request));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<?>> confirmOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.confirmOrder(authHeader, id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.cancelOrder(authHeader, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Order order) {
        return ResponseEntity.ok(application.updateOrder(authHeader, id, order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteOrder(authHeader, id));
    }
}
