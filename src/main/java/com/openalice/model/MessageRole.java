package com.openalice.model;

/** 一条 {@link ChatMessage} 的发言者。 */
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String wireValue;

    MessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    /** 小写形式的传输 / 展示值，例如 {@code "user"}。 */
    public String wireValue() {
        return wireValue;
    }
}
