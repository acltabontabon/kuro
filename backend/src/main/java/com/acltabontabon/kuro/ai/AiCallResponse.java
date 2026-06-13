package com.acltabontabon.kuro.ai;

import java.time.Duration;

/**
 * The outcome of a structured-generation call (issue #17), carrying enough
 * metadata to satisfy {@code AiRun} auditing. {@code rawJson} is the
 * unvalidated string the provider returned — the caller validates it against
 * the request schema (#21). {@code providerCallId} is the vendor's correlation
 * id for the call, useful for tracing without leaking vendor types.
 */
public record AiCallResponse(
        String rawJson,
        int inputTokens,
        int outputTokens,
        Duration latency,
        String providerCallId) {
}
