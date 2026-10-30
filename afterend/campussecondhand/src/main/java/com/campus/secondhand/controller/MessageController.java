package com.campus.secondhand.controller;

import com.campus.secondhand.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    @Autowired
    private com.campus.secondhand.service.MessageApplicationService application;

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<?>> receivedMessages(@RequestHeader("Authorization") String authHeader, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize) {
        return ResponseEntity.ok(application.receivedMessages(authHeader, page, pageSize));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<?>> sentMessages(@RequestHeader("Authorization") String authHeader, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize) {
        return ResponseEntity.ok(application.sentMessages(authHeader, page, pageSize));
    }

    @GetMapping("/conversations")
    public ApiResponse<?> conversations(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize,
            @RequestParam(required=false) String search, @RequestParam(defaultValue="false") boolean unread) {
        return application.conversations(page,pageSize,search,unread);
    }
    @GetMapping("/conversation/{otherUserId}")
    public ApiResponse<?> conversation(@PathVariable Long otherUserId, @RequestHeader("Authorization") String authHeader,
            @RequestParam(required=false) Long before, @RequestParam(required=false) Long after) {
        return application.conversation(otherUserId,authHeader,before,after);
    }

    @PutMapping("/conversation/{otherUserId}/read")
    public ResponseEntity<ApiResponse<?>> markConversationRead(
            @PathVariable Long otherUserId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.markConversationRead(otherUserId, authHeader));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<?>> unreadCount(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.unreadCount(authHeader));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<?>> send(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey,
            @RequestBody Map<String, Object> params) {
        return ResponseEntity.ok(application.send(authHeader, requestKey, params));
    }

    @PutMapping({"/read/{id}", "/{id}/read"})
    public ResponseEntity<ApiResponse<?>> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.markAsRead(id, authHeader));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.markAllAsRead(authHeader));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(application.delete(id, authHeader));
    }
}
