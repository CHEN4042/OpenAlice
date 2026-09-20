package com.openalice.chat.store.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.openalice.chat.store.StoredMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class PostgresConversationStoreTest {

    private JdbcClient jdbcClient;
    private PostgresConversationStore store;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:openalice-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        createSchema(dataSource);
        jdbcClient = JdbcClient.create(dataSource);
        store = new PostgresConversationStore(jdbcClient);
    }

    @Test
    void shouldRestoreHistoryWithANewStoreInstance() {
        store.append(message("user-1", "session-a", "user", "今天理发了", "2026-09-20T01:00:00Z"));
        store.append(message("user-1", "session-a", "assistant", "剪成什么样？", "2026-09-20T01:00:01Z"));

        PostgresConversationStore restartedStore = new PostgresConversationStore(jdbcClient);
        List<StoredMessage> history = restartedStore.history("session-a", 20);

        assertThat(history).extracting(StoredMessage::content)
                .containsExactly("今天理发了", "剪成什么样？");
        assertThat(history).extracting(StoredMessage::role)
                .containsExactly(com.openalice.model.MessageRole.USER, com.openalice.model.MessageRole.ASSISTANT);
    }

    @Test
    void shouldIsolateSessionsAndKeepTheLatestMessagesInChronologicalOrder() {
        store.append(message("user-1", "session-a", "user", "第一条", "2026-09-20T01:00:00Z"));
        store.append(message("user-1", "session-a", "assistant", "第二条", "2026-09-20T01:00:01Z"));
        store.append(message("user-1", "session-a", "user", "第三条", "2026-09-20T01:00:02Z"));
        store.append(message("user-1", "session-b", "user", "另一个会话", "2026-09-20T01:00:03Z"));

        assertThat(store.history("session-a", 2)).extracting(StoredMessage::content)
                .containsExactly("第二条", "第三条");
        assertThat(store.history("session-b", 20)).extracting(StoredMessage::content)
                .containsExactly("另一个会话");
    }

    @Test
    void shouldReturnEmptyHistoryForNonPositiveLimit() {
        assertThat(store.history("session-a", 0)).isEmpty();
        assertThat(store.history("session-a", -1)).isEmpty();
    }

    @Test
    void shouldRejectBlankSessionId() {
        assertThatIllegalArgumentException().isThrownBy(() -> store.history(" ", 20));
    }

    private static StoredMessage message(
            String userId,
            String sessionId,
            String role,
            String content,
            String createdAt
    ) {
        return new StoredMessage(
                UUID.randomUUID().toString(),
                userId,
                sessionId,
                new com.openalice.model.ChatMessage(
                        com.openalice.model.MessageRole.valueOf(role.toUpperCase()),
                        content
                ),
                Instant.parse(createdAt)
        );
    }

    private static void createSchema(DataSource dataSource) throws Exception {
        try (var connection = dataSource.getConnection()) {
            String sql = new String(
                    new ClassPathResource("db/migration/V1__create_session_message.sql")
                            .getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
