package com.campus.secondhand.controller;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LayeringTest {
    @Test void controllersNeverAccessPersistenceOrDeclareTransactions() throws Exception {
        try (var files = Files.list(Path.of("src/main/java/com/campus/secondhand/controller"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.matches("(?s).*(Repository|JdbcTemplate|QueryWrapper|UpdateWrapper|@Transactional).*"), path.toString());
            }
        }
    }
}
