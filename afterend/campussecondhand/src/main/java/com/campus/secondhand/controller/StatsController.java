package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {
    @Autowired
    private com.campus.secondhand.service.StatsApplicationService application;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> stats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(application.stats(authHeader));
    }
}
