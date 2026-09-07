package com.openalice.agent;

/** Agent runtime event; use concrete records instead of transporting raw strings. */
public sealed interface AgentEvent permits TextDeltaEvent, DoneEvent, ErrorEvent {
}
