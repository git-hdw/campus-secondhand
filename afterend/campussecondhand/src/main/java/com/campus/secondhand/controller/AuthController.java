package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private com.campus.secondhand.service.AuthApplicationService application;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody Map<String, String> credentials) {
        return ResponseEntity.ok(application.login(credentials));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(application.register(userDTO));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword() {
        return ResponseEntity.ok(application.forgotPassword());
    }
}
