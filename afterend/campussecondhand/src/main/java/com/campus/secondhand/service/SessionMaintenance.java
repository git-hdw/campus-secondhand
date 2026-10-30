package com.campus.secondhand.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
@EnableScheduling
public class SessionMaintenance {
    private final JdbcTemplate jdbc;
    public SessionMaintenance(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    /** Bounded cleanup only: active sessions and request-dedup records are never removed. */
    @Scheduled(initialDelay = 3600000, fixedDelay = 3600000)
    public int cleanup() {
        return jdbc.update("DELETE FROM auth_sessions WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL 30 DAY OR revoked_at < CURRENT_TIMESTAMP - INTERVAL 30 DAY LIMIT 1000");
    }
}
