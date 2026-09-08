package com.openalice.chat.store;

import java.util.List;

/**
 * 对话持久化端口（存储抽象）。
 *
 * <p>存储是业务对话历史的唯一事实来源。AgentScope 的状态存储只是运行时暂存，
 * 每次调用前会被清空。外部 API 是单用户的，历史按 {@code sessionId} 读取；
 * 存储记录仍携带 {@code userId}，为将来接入认证预留。</p>
 */
public interface ConversationStore {

    void append(StoredMessage message);

    List<StoredMessage> history(String sessionId, int limit);
}
