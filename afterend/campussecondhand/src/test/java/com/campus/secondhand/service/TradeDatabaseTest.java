package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.exception.TradeStateException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in tests create and drop only their uniquely named isolated database. */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "review.integration", matches = "true")
class TradeDatabaseTest {
    private static final String DATABASE = "campus_review_" + UUID.randomUUID().toString().replace("-", "");
    private static final Properties LOCAL = settings();
    private static final String USER = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", LOCAL.getProperty("DB_PASSWORD"));
    private static final String URL = "jdbc:mysql://localhost:3306/" + DATABASE + "?createDatabaseIfNotExist=true&serverTimezone=Asia/Shanghai";
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderTradeService trade;
    @Autowired FavoriteService favorites;
    @Autowired IdempotencyService idempotency;
    @Autowired MessageQueryService messageQueries;
    @Autowired ProductService productQueries;
    @Autowired SessionMaintenance sessionMaintenance;
    @Autowired com.campus.secondhand.config.SchemaMigration migration;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.campus.secondhand.util.JwtUtil tokens;
    @Autowired com.campus.secondhand.repository.UserRepository users;
    private static Properties settings() {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(Path.of(".local/runtime.properties"))) { properties.load(in); }
        catch (Exception e) { throw new IllegalStateException("Isolated test requires local DB settings", e); }
        return properties;
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("jwt.secret", () -> "isolated-integration-test-key-".repeat(4));
    }
    @BeforeEach void setup() {
        jdbc.update("DELETE FROM auth_sessions");
        for (int id = 2; id < 120; id++) jdbc.update("INSERT IGNORE INTO users(id,username,email,password) VALUES(?,?,?,?)", id, "testuser" + id, "testuser" + id + "@local", "test-hash");
        jdbc.update("DELETE FROM request_dedup");
        jdbc.update("DELETE FROM favorites"); jdbc.update("DELETE FROM messages");
        jdbc.update("DELETE FROM orders"); jdbc.update("DELETE FROM products");
        jdbc.update("INSERT INTO products(id,user_id,title,description,price,category,`condition`,trade_place,status,created_time) VALUES(900,3,'测试商品','仅用于独立测试库',10,'books','全新','图书馆门口',0,NOW())");
    }
    @AfterAll static void cleanup() throws Exception {
        if (!DATABASE.matches("campus_review_[a-f0-9]{32}")) throw new IllegalStateException("Unexpected test database");
        try (var connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", USER, PASSWORD);
             var statement = connection.createStatement()) { statement.execute("DROP DATABASE `" + DATABASE + "`"); }
    }
    private User user(long id) { User user = new User(); user.setId(id); return user; }
    private List<ApiResponse<?>> parallel(List<Callable<ApiResponse<?>>> tasks) throws Exception {
        var executor = Executors.newFixedThreadPool(tasks.size());
        var barrier = new CyclicBarrier(tasks.size());
        try {
            List<Future<ApiResponse<?>>> futures = new ArrayList<>();
            for (var task : tasks) futures.add(executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try { return task.call(); }
                catch (TradeStateException e) { return ApiResponse.error(400, e.getMessage()); }
            }));
            List<ApiResponse<?>> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally { executor.shutdownNow(); }
    }
    @Test void decimalProductIdIsRejectedBeforeCreatingAnOrder() throws Exception {
        User admin = users.findByUsername("admin").orElseThrow();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/orders")
                .header("Authorization", "Bearer " + tokens.generateToken(admin))
                .contentType("application/json").content("{\"productId\":900.9}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
    }
    @Test void logoutRevokesOnlyTheSelectedSessionAndCannotRevokeAnotherUser() throws Exception {
        User owner = users.selectById(2L), other = users.selectById(3L);
        String first = tokens.generateToken(owner), second = tokens.generateToken(owner);
        String foreign = tokens.generateToken(other);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/auth/sessions/" + tokens.tokenId("Bearer " + foreign))
                .header("Authorization", "Bearer " + first))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/logout")
                .header("Authorization", "Bearer " + first))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertNull(tokens.resolve(first));
        assertNotNull(tokens.resolve(second)); assertNotNull(tokens.resolve(foreign));
    }
    @Test void logoutAllRevokesEverySessionOfTheActor() throws Exception {
        User owner = users.selectById(2L);
        String first = tokens.generateToken(owner), second = tokens.generateToken(owner);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/logout-all")
                .header("Authorization", "Bearer " + first))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertNull(tokens.resolve(first)); assertNull(tokens.resolve(second));
    }
    @Test void databaseRejectsOrphanReferencesAndDuplicatePendingOrders() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO favorites(user_id,product_id) VALUES(2,999999)"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM users WHERE id=3"));
        trade.place(user(2), 900L);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO orders(order_no,buyer_id,seller_id,product_id,price,status) VALUES('duplicate',4,3,900,20,0)"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
    }
    @Test void migrationPreservesLegacyOrderAndCanRunAgain() {
        jdbc.execute("ALTER TABLE orders DROP FOREIGN KEY fk_orders_product_id");
        jdbc.update("INSERT INTO orders(order_no,buyer_id,seller_id,product_id,price,status) VALUES('legacy',2,3,999999,20,4)");
        jdbc.update("DELETE FROM schema_migrations WHERE version=1");
        migration.run(null); migration.run(null);
        var row = jdbc.queryForMap("SELECT product_id,legacy_product_id,price,status FROM orders WHERE order_no='legacy'");
        assertNull(row.get("product_id")); assertEquals(999999L, ((Number)row.get("legacy_product_id")).longValue());
        assertEquals(4, ((Number)row.get("status")).intValue());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
    }
    @Test void historyUsesBoundedCursorsAndSearchFindsOlderMessages() {
        for (int i=0;i<65;i++) jdbc.update("INSERT INTO messages(sender_id,receiver_id,product_id,content,created_time) VALUES(2,3,900,?,NOW())", i == 0 ? "older special phrase" : "message " + i);
        var recent = messageQueries.history(3,2,null,null);
        var items = (List<com.campus.secondhand.entity.Message>)recent.get("items");
        assertEquals(50,items.size()); assertEquals(true,recent.get("hasMore"));
        var older = messageQueries.history(3,2,items.get(0).getId(),null);
        assertEquals(15, ((List<?>)older.get("items")).size());
        assertEquals(0, ((List<?>)messageQueries.history(3,2,null,items.get(49).getId()).get("items")).size());
        assertEquals(1, messageQueries.conversations(3,1,20,"older special",false).total());
        assertEquals(0, messageQueries.conversations(4,1,20,"older special",false).total());
        assertEquals(20, messageQueries.page(3,true,1,20).items().size());
    }
    @Test void personalListsArePagedAndOrderLookupFiltersAcrossPages() throws Exception {
        for (int i=0;i<32;i++) jdbc.update("INSERT INTO orders(order_no,buyer_id,seller_id,product_id,price,status,created_time) VALUES(?,2,3,900,20,4,NOW())", "history" + i);
        String token = tokens.generateToken(users.selectById(2L));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/my?page=2&pageSize=20")
                .header("Authorization","Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.total").value(32))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.items.length()").value(12));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/my?productId=900&status=0")
                .header("Authorization","Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.total").value(0));
    }
    @Test void concurrentBuyersCreateOnlyOnePendingOrder() throws Exception {
        List<Callable<ApiResponse<?>>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) { final long buyer = 100 + i; tasks.add(() -> trade.place(user(buyer), 900L)); }
        assertEquals(1, parallel(tasks).stream().filter(r -> r.getCode() == 200).count());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE product_id=900 AND status=0", Integer.class));
    }
    @Test void snapshotPreservesThePurchasedDescriptionAfterProductChanges() {
        trade.place(user(2),900L);
        String snapshot = jdbc.queryForObject("SELECT snapshot_json FROM orders", String.class);
        var before = com.campus.secondhand.util.OrderSnapshot.read(snapshot);
        jdbc.update("UPDATE products SET title='later title',trade_place='later place' WHERE id=900");
        assertNotEquals("later title", before.path("title").asText());
        assertEquals(snapshot, jdbc.queryForObject("SELECT snapshot_json FROM orders", String.class));
    }
    @Test void personalProductsKeepGlobalCountsWhilePaging() {
        for (int i=0;i<22;i++) jdbc.update("INSERT INTO products(user_id,title,price,category,status,view_count) VALUES(3,?,10,'books',0,2)", "page item " + i);
        var result = productQueries.personalPage(3L,2,20);
        assertEquals(23L,result.get("total"));
        assertEquals(3,((List<?>)result.get("items")).size());
        assertEquals(23,((Number)((Map<?,?>)result.get("stats")).get("listings")).intValue());
    }
    @Test void maintenanceKeepsActiveSessionsAndRetryProtection() {
        String active = tokens.generateToken(users.selectById(2L));
        jdbc.update("INSERT INTO auth_sessions(id,user_id,expires_at) VALUES('old',2,CURRENT_TIMESTAMP - INTERVAL 31 DAY)");
        jdbc.update("INSERT INTO request_dedup(user_id,scope,request_key,payload_hash) VALUES(2,'test','retained-key','hash')");
        assertEquals(1,sessionMaintenance.cleanup());
        assertNotNull(tokens.resolve(active));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM request_dedup",Integer.class));
    }
    @Test void concurrentConfirmAndCancelKeepProductConsistent() throws Exception {
        trade.place(user(2), 900L);
        Long id = jdbc.queryForObject("SELECT id FROM orders", Long.class);
        var results = parallel(List.of(() -> trade.changeOrder(user(2), id, 3), () -> trade.changeOrder(user(3), id, 4)));
        assertEquals(1, results.stream().filter(r -> r.getCode() == 200).count());
        int state = jdbc.queryForObject("SELECT status FROM orders WHERE id=?", Integer.class, id);
        assertEquals(state == 3 ? 1 : 0, jdbc.queryForObject("SELECT status FROM products WHERE id=900", Integer.class));
    }
    @Test void failedOrderTransitionRollsBackProductUpdate() {
        trade.place(user(2), 900L);
        Long id = jdbc.queryForObject("SELECT id FROM orders", Long.class);
        jdbc.update("UPDATE orders SET status=4 WHERE id=?", id);
        assertThrows(TradeStateException.class, () -> trade.changeOrder(user(2), id, 3));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM products WHERE id=900", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT status FROM orders WHERE id=?", Integer.class, id));
    }
    @Test void legacyPlaceCanBeKeptButNotReplacedWithArbitraryPlace() {
        assertEquals(200, trade.editProduct(user(3), 900L, Map.of("title", "修改标题", "tradePlace", "图书馆门口")).getCode());
        assertEquals(400, trade.editProduct(user(3), 900L, Map.of("tradePlace", "任意新地点")).getCode());
    }
    @Test void duplicateFavoritesAreIdempotentAndCleanedWithProduct() throws Exception {
        var results = parallel(List.of(() -> favorites.create(user(2), 900L), () -> favorites.create(user(2), 900L)));
        assertTrue(results.stream().allMatch(r -> r.getCode() == 200));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM favorites", Integer.class));
        assertEquals(200, trade.deleteProduct(user(3), 900L).getCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM favorites", Integer.class));
    }
    @Test void retryKeyCommitsOneWriteAndRejectsChangedPayload() throws Exception {
        var payload = Map.<String, Object>of("content", "hello");
        Callable<ApiResponse<?>> send = () -> idempotency.execute(2L, "message", "test-request-00000001", payload, () -> {
            jdbc.update("INSERT INTO messages(sender_id,receiver_id,content) VALUES(2,3,'hello')");
            return ApiResponse.success("ok", Map.of("id", 99));
        });
        assertTrue(parallel(List.of(send, send)).stream().allMatch(r -> r.getCode() == 200));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class));
        assertEquals(409, idempotency.execute(2L, "message", "test-request-00000001", Map.of("content", "changed"), () -> { fail("must not run"); return null; }).getCode());
    }
    @Test void authenticatedHttpUsesNewTokenAndRejectsDeletedAccounts() throws Exception {
        User admin = users.findByUsername("admin").orElseThrow();
        String token = tokens.generateToken(admin);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/user/profile")
            .header("Authorization", "Bearer " + token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/user/profile")
            .header("Authorization", "Bearer " + token).contentType("application/json").content("{\"username\":null}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        jdbc.update("UPDATE users SET deleted=1 WHERE id=?", admin.getId());
        try {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/user/profile")
                .header("Authorization", "Bearer " + token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        } finally { jdbc.update("UPDATE users SET deleted=0 WHERE id=?", admin.getId()); }
    }
}
