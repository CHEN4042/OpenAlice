package io.openalice.execution;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutionRepository {

    private final JdbcClient jdbcClient;

    public ExecutionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(UUID executionId, Instant now) {
        jdbcClient
                .sql("""
                        INSERT INTO executions(execution_id, status, created_at, updated_at)
                        VALUES (:id, :status, :createdAt, :updatedAt)
                        """)
                .param("id", executionId.toString())
                .param("status", ExecutionStatus.RUNNING.name())
                .param("createdAt", now.toString())
                .param("updatedAt", now.toString())
                .update();
    }

    public Optional<Execution> find(UUID executionId) {
        return jdbcClient
                .sql("""
                        SELECT execution_id, status, created_at, updated_at
                        FROM executions
                        WHERE execution_id = :id
                        """)
                .param("id", executionId.toString())
                .query((resultSet, rowNumber) -> new Execution(
                        UUID.fromString(resultSet.getString("execution_id")),
                        ExecutionStatus.valueOf(resultSet.getString("status")),
                        Instant.parse(resultSet.getString("created_at")),
                        Instant.parse(resultSet.getString("updated_at"))))
                .optional();
    }

    public boolean transitionFromRunning(
            UUID executionId, ExecutionStatus terminalStatus, Instant now) {
        if (terminalStatus == ExecutionStatus.RUNNING) {
            throw new IllegalArgumentException("Terminal status must not be RUNNING");
        }
        return jdbcClient
                        .sql("""
                                UPDATE executions
                                SET status = :status, updated_at = :updatedAt
                                WHERE execution_id = :id AND status = :running
                                """)
                        .param("status", terminalStatus.name())
                        .param("updatedAt", now.toString())
                        .param("id", executionId.toString())
                        .param("running", ExecutionStatus.RUNNING.name())
                        .update()
                == 1;
    }
}
