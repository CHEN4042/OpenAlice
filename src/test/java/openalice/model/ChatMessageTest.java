package openalice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatMessageTest {

    @Test
    void shouldCreateUserMessage() {
        var message = ChatMessage.user(UserId.of("u"), SessionId.of("s"), "hello");

        assertThat(message.id()).isNotBlank();
        assertThat(message.role()).isEqualTo(MessageRole.USER);
        assertThat(message.content()).isEqualTo("hello");
        assertThat(message.timestamp()).isNotNull();
    }

    @Test
    void shouldRejectBlankUserId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UserId.of(" "));
    }
}

