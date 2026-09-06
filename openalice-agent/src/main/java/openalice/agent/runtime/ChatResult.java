package openalice.agent.runtime;

import openalice.core.domain.SessionId;
import openalice.core.domain.UserId;

public record ChatResult(
        UserId userId,
        SessionId sessionId,
        String reply
) {
}

