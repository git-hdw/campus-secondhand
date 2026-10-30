package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/** The claim, business write and replay response commit in the same database transaction. */
@Service
public class IdempotencyService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public IdempotencyService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
    @Transactional
    public ApiResponse<?> execute(Long actor, String scope, String key, Map<String, Object> payload, Supplier<ApiResponse<?>> action) {
        if (key == null) return action.get(); // Compatibility for older API clients.
        if (!key.matches("[A-Za-z0-9_-]{16,80}")) throw new IllegalArgumentException("请求标识格式不正确");
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    json.writeValueAsString(new TreeMap<>(payload)).getBytes(StandardCharsets.UTF_8)));
            jdbc.update("INSERT INTO request_dedup(user_id,scope,request_key,payload_hash) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE request_key=request_key", actor, scope, key, hash);
            Map<String, Object> row = jdbc.queryForMap("SELECT payload_hash,response_json FROM request_dedup WHERE user_id=? AND scope=? AND request_key=? FOR UPDATE", actor, scope, key);
            if (!hash.equals(row.get("payload_hash"))) return ApiResponse.error(409, "请求内容已变化，请重新提交");
            if (row.get("response_json") != null) return json.readValue(row.get("response_json").toString(), ApiResponse.class);
            ApiResponse<?> result = action.get();
            jdbc.update("UPDATE request_dedup SET response_json=? WHERE user_id=? AND scope=? AND request_key=?", json.writeValueAsString(result), actor, scope, key);
            return result;
        } catch (java.security.GeneralSecurityException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to persist request result", e);
        }
    }
}
