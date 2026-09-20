package com.openalice.chat.store.postgres;

import com.openalice.chat.store.ConversationStore;
import com.openalice.chat.store.StoredMessage;
import com.openalice.model.ChatMessage;
import com.openalice.model.MessageRole;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link ConversationStore} 的 PostgreSQL 实现。
 *
 * <p>每次追加只写一行；读取历史时先按时间倒序取最近 {@code limit} 条，再恢复
 * 为升序，保证发往模型的上下文顺序与用户实际对话一致。表结构由 Flyway
 * 迁移维护，业务层不感知 SQL。</p>
 */
public final class PostgresConversationStore implements ConversationStore {

    private static final String INSERT_SQL = """
            INSERT INTO session_message (id, user_id, session_id, role, content, created_at)
            VALUES (:id, :userId, :sessionId, :role, :content, :createdAt)
            """;

    private static final String HISTORY_SQL = """
            SELECT recent.id, recent.user_id, recent.session_id, recent.role,
                   recent.content, recent.created_at
            FROM (
                SELECT id, user_id, session_id, role, content, created_at
                FROM session_message
                WHERE session_id = :sessionId
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
            ) recent
            ORDER BY recent.created_at ASC, recent.id ASC
            """;

    private final JdbcClient jdbcClient;

    public PostgresConversationStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void append(StoredMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        jdbcClient.sql(INSERT_SQL)
                .param("id", message.id())
                .param("userId", message.userId())
                .param("sessionId", message.sessionId())
                .param("role", message.role().wireValue())
                .param("content", message.content())
                .param("createdAt", Timestamp.from(message.createdAt()))
                .update();
    }

    @Override
    public List<StoredMessage> history(String sessionId, int limit) {
        requireNotBlank(sessionId, "sessionId");
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient.sql(HISTORY_SQL)
                .param("sessionId", sessionId)
                .param("limit", limit)
                .query((resultSet, rowNumber) -> new StoredMessage(
                        resultSet.getString("id"),
                        resultSet.getString("user_id"),
                        resultSet.getString("session_id"),
                        new ChatMessage(
                                parseRole(resultSet.getString("role")),
                                resultSet.getString("content")
                        ),
                        resultSet.getTimestamp("created_at").toInstant()
                ))
                .list();
    }

    private static MessageRole parseRole(String value) {
        try {
            return MessageRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("unknown message role: " + value, error);
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
