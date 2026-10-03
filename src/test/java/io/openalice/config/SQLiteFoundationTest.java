package io.openalice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.main.banner-mode=off")
class SQLiteFoundationTest {

    @TempDir static Path openAliceHome;

    @DynamicPropertySource
    static void configureHome(DynamicPropertyRegistry registry) {
        registry.add("openalice.home", openAliceHome::toString);
    }

    @Autowired JdbcClient jdbcClient;
    @Autowired Flyway flyway;

    @Test
    void databaseUsesOpenAliceHomeAndRequiredPragmas() {
        assertThat(Files.isRegularFile(openAliceHome.resolve("data/openalice.db"))).isTrue();
        assertThat(jdbcClient.sql("PRAGMA journal_mode").query(String.class).single())
                .isEqualToIgnoringCase("wal");
        assertThat(jdbcClient.sql("PRAGMA foreign_keys").query(Integer.class).single()).isOne();
        assertThat(jdbcClient.sql("PRAGMA busy_timeout").query(Integer.class).single())
                .isEqualTo(5_000);
    }

    @Test
    void flywayMigrationIsAppliedOnceAndCreatesExecutionTable() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbcClient
                        .sql("""
                                SELECT count(*)
                                FROM sqlite_master
                                WHERE type = 'table' AND name = 'executions'
                                """)
                        .query(Integer.class)
                        .single())
                .isOne();
    }

    @Test
    void sqliteSnapshotCanBeOpenedAndRestored() throws Exception {
        UUID executionId = UUID.randomUUID();
        String now = Instant.now().toString();
        jdbcClient
                .sql("""
                        INSERT INTO executions(execution_id, status, created_at, updated_at)
                        VALUES (:id, 'RUNNING', :createdAt, :updatedAt)
                        """)
                .param("id", executionId.toString())
                .param("createdAt", now)
                .param("updatedAt", now)
                .update();

        Path snapshot = openAliceHome.resolve("openalice-backup.db");
        jdbcClient.sql("VACUUM INTO :snapshot").param("snapshot", snapshot.toString()).update();

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
                var statement = connection.prepareStatement(
                        "SELECT status FROM executions WHERE execution_id = ?")) {
            statement.setString(1, executionId.toString());
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("status")).isEqualTo("RUNNING");
            }
        }
    }
}
