package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    @Autowired
    private com.campus.secondhand.service.CategoryApplicationService application;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllCategories() {
        return ResponseEntity.ok(application.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(application.getCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Category category) {
        return ResponseEntity.ok(application.createCategory(authHeader, category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id, 
            @RequestBody Category category) {
        return ResponseEntity.ok(application.updateCategory(authHeader, id, category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteCategory(authHeader, id));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<?>> getCategoriesByParentId(@PathVariable Long parentId) {
        return ResponseEntity.ok(application.getCategoriesByParentId(parentId));
    }
}
