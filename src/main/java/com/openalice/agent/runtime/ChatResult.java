package com.openalice.agent.runtime;

import com.openalice.model.SessionId;
import com.openalice.model.UserId;

public record ChatResult(
        UserId userId,
        SessionId sessionId,
        String reply
) {
}
