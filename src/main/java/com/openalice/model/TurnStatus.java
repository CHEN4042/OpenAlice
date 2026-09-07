package com.openalice.model;

/** Lifecycle of one request/response turn. */
public enum TurnStatus {
    RECEIVED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
