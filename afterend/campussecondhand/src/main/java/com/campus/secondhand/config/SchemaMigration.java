package com.campus.secondhand.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Idempotent migration: preserve orphaned order references, then enforce relational integrity. */
@Component
public class SchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private void column(String table, String column, String definition) {
        if (count("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?", table, column) == 0)
            jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
    }
    private void unique(String table, String name, String column) {
        if (count("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND INDEX_NAME=?", table, name) == 0)
            jdbc.execute("ALTER TABLE `" + table + "` ADD UNIQUE KEY `" + name + "` (`" + column + "`)");
    }
    private void reference(String table, String field, String parent, String key) {
        String name = "fk_" + table + "_" + field;
        if (count("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=? AND CONSTRAINT_NAME=?", table, name) > 0) return;
        if (count("SELECT COUNT(*) FROM `" + table + "` c LEFT JOIN `" + parent + "` p ON c.`" + field + "`=p.`" + key + "` WHERE c.`" + field + "` IS NOT NULL AND p.`" + key + "` IS NULL") > 0)
            throw new IllegalStateException("Migration requires relation repair: " + table + "." + field);
        jdbc.execute("ALTER TABLE `" + table + "` ADD CONSTRAINT `" + name + "` FOREIGN KEY (`" + field + "`) REFERENCES `" + parent + "` (`" + key + "`) ON DELETE RESTRICT ON UPDATE RESTRICT");
    }
    @Override public void run(ApplicationArguments args) {
        migrateReferences();
        if (count("SELECT COUNT(*) FROM schema_migrations WHERE version=2") == 0) {
            column("orders", "snapshot_json", "MEDIUMTEXT NULL");
            jdbc.update("INSERT INTO schema_migrations(version) VALUES(2)");
        }
    }
    private void migrateReferences() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS schema_migrations(version INT PRIMARY KEY,applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
        if (count("SELECT COUNT(*) FROM schema_migrations WHERE version=1") > 0) return;
        for (String table : java.util.List.of("users","categories","products","orders","favorites","messages","request_dedup","auth_sessions")) {
            String engine = jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, table);
            if (!"InnoDB".equalsIgnoreCase(engine)) jdbc.execute("ALTER TABLE `" + table + "` ENGINE=InnoDB");
        }
        column("orders", "legacy_product_id", "BIGINT NULL COMMENT 'Original ID when historical product no longer exists'");
        jdbc.execute("ALTER TABLE orders MODIFY product_id BIGINT NULL");
        // One atomic statement preserves the original ID before clearing an impossible reference.
        jdbc.update("UPDATE orders o LEFT JOIN products p ON p.id=o.product_id SET o.legacy_product_id=o.product_id,o.product_id=NULL WHERE o.product_id IS NOT NULL AND p.id IS NULL");
        reference("products", "user_id", "users", "id");
        reference("products", "category", "categories", "name");
        reference("orders", "buyer_id", "users", "id");
        reference("orders", "seller_id", "users", "id");
        reference("orders", "product_id", "products", "id");
        reference("favorites", "user_id", "users", "id");
        reference("favorites", "product_id", "products", "id");
        reference("messages", "sender_id", "users", "id");
        reference("messages", "receiver_id", "users", "id");
        reference("messages", "product_id", "products", "id");
        reference("categories", "parent_id", "categories", "id");
        reference("request_dedup", "user_id", "users", "id");
        reference("auth_sessions", "user_id", "users", "id");
        column("orders", "pending_product_id", "BIGINT GENERATED ALWAYS AS (CASE WHEN status=0 THEN product_id ELSE NULL END) VIRTUAL");
        column("orders", "completed_product_id", "BIGINT GENERATED ALWAYS AS (CASE WHEN status=3 THEN product_id ELSE NULL END) VIRTUAL");
        unique("orders", "uk_orders_pending_product", "pending_product_id");
        unique("orders", "uk_orders_completed_product", "completed_product_id");
        jdbc.update("INSERT INTO schema_migrations(version) VALUES(1)");
    }
}
