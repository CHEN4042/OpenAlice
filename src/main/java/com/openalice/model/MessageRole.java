package com.openalice.model;

/** Speaker of a {@link ChatMessage}. */
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String wireValue;

    MessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Lower-case transport / display value, e.g. {@code "user"}. */
    public String wireValue() {
        return wireValue;
    }
}
