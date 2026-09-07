package openalice.agent.runtime;

import openalice.model.SessionId;
import openalice.model.UserId;

public record ChatResult(
        UserId userId,
        SessionId sessionId,
        String reply
) {
}

