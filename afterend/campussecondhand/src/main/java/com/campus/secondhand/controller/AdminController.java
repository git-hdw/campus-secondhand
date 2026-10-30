package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    @Autowired
    private com.campus.secondhand.service.AdminApplicationService application;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<?>> getAllUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseEntity.ok(application.getAllUsers(authHeader, search, page, pageSize));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> getUserDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.getUserDetail(authHeader, id));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateUserStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        return ResponseEntity.ok(application.updateUserStatus(authHeader, id, status));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<?>> getAllProducts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseEntity.ok(application.getAllProducts(authHeader, search, category, status, page, pageSize));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<?>> getProductDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.getProductDetail(authHeader, id));
    }

    @PutMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        return ResponseEntity.ok(application.updateProductStatus(authHeader, id, status));
    }

    @PutMapping("/products/batch/status")
    public ResponseEntity<ApiResponse<?>> batchUpdateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params) {
        return ResponseEntity.ok(application.batchUpdateProductStatus(authHeader, params));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<?>> getAllOrders(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseEntity.ok(application.getAllOrders(authHeader, search, status, page, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<?>> getOrderDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.getOrderDetail(authHeader, id));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(application.updateOrderStatus(authHeader, id, params));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<?>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteOrder(authHeader, id));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> getAllMessages(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseEntity.ok(application.getAllMessages(authHeader, search, page, pageSize));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<?>> deleteMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteMessage(authHeader, id));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<?>> getAllCategories(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.getAllCategories(authHeader));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<?>> addCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(application.addCategory(authHeader, params));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<?>> updateCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(application.updateCategory(authHeader, id, params));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteCategory(authHeader, id));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getStats(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.getStats(authHeader));
    }
}
