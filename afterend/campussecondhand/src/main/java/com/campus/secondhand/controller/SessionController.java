package com.campus.secondhand.controller;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.service.SessionApplicationService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class SessionController {
    private final SessionApplicationService service;
    public SessionController(SessionApplicationService service) { this.service = service; }
    @GetMapping("/sessions") public ApiResponse<?> list(@RequestHeader("Authorization") String header) { return service.list(header); }
    @DeleteMapping("/sessions/{id}") public ApiResponse<?> revoke(@PathVariable String id) { return service.revoke(id); }
    @PostMapping("/logout") public ApiResponse<?> logout(@RequestHeader("Authorization") String header) { return service.logout(header); }
    @PostMapping("/logout-all") public ApiResponse<?> logoutAll() { return service.logoutAll(); }
}
