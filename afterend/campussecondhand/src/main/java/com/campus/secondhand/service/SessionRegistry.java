package com.campus.secondhand.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** One revocable record per login; never stores a raw bearer token. */
@Service
public class SessionRegistry {
    private final JdbcTemplate jdbc;
    public SessionRegistry(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void register(long userId, String id, Date expires) {
        jdbc.update("INSERT INTO auth_sessions(id,user_id,expires_at) VALUES(?,?,?)", id, userId, new Timestamp(expires.getTime()));
    }
    public boolean active(long userId, String id) {
        return id != null && jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE id=? AND user_id=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP", Integer.class, id, userId) == 1;
    }
    public List<Map<String, Object>> list(long userId, String current) {
        var rows = jdbc.queryForList("SELECT id,created_at AS createdAt,expires_at AS expiresAt FROM auth_sessions WHERE user_id=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP ORDER BY created_at DESC,id DESC LIMIT 100", userId);
        rows.forEach(row -> row.put("current", row.get("id").equals(current)));
        return rows;
    }
    public boolean revoke(long userId, String id) {
        return jdbc.update("UPDATE auth_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=? AND revoked_at IS NULL", id, userId) > 0;
    }
    public void revokeAll(long userId) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=? AND revoked_at IS NULL", userId);
    }
}
