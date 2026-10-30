package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {
    @Autowired
    private com.campus.secondhand.service.FavoriteApplicationService application;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyFavorites(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(application.getMyFavorites(authHeader, page, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getFavoriteById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.getFavoriteById(authHeader, id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createFavorite(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Favorite favorite) {
        return ResponseEntity.ok(application.createFavorite(authHeader, favorite));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(application.deleteFavorite(authHeader, id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> checkFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        return ResponseEntity.ok(application.checkFavorite(authHeader, productId));
    }
}
