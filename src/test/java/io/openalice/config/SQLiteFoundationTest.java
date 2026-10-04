package io.openalice.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.openalice.mapper.ExecutionMapper;
import io.openalice.model.Execution;
import io.openalice.model.ExecutionStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.main.banner-mode=off")
class SQLiteFoundationTest {

    @TempDir static Path openAliceHome;

    @DynamicPropertySource
    static void configureHome(DynamicPropertyRegistry registry) {
        registry.add("openalice.home", openAliceHome::toString);
    }

    @Autowired DataSource dataSource;
    @Autowired ExecutionMapper executionMapper;
    @Autowired Flyway flyway;

    @Test
    void databaseUsesOpenAliceHomeAndRequiredPragmas() throws Exception {
        assertThat(Files.isRegularFile(openAliceHome.resolve("data/openalice.db"))).isTrue();
        assertThat(queryString("PRAGMA journal_mode")).isEqualToIgnoringCase("wal");
        assertThat(queryInt("PRAGMA foreign_keys")).isOne();
        assertThat(queryInt("PRAGMA busy_timeout")).isEqualTo(5_000);
    }

    @Test
    void flywayMigrationIsAppliedOnceAndCreatesExecutionTable() throws Exception {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(queryInt("""
                SELECT count(*)
                FROM sqlite_master
                WHERE type = 'table' AND name = 'executions'
                """))
                .isOne();
    }

    @Test
    void sqliteSnapshotCanBeOpenedAndRestored() throws Exception {
        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();
        assertThat(executionMapper.insert(
                        new Execution(executionId, ExecutionStatus.RUNNING, now, now)))
                .isOne();

        Path snapshot = openAliceHome.resolve("openalice-backup.db");
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, snapshot.toString());
            statement.executeUpdate();
        }

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

    private String queryString(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private int queryInt(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
