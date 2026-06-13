package com.acltabontabon.kuro.ai;

import java.time.Duration;

/**
 * Per-call tuning knobs (issue #17). Every field is nullable; a {@code null}
 * means "use the provider's configured default" rather than an explicit value.
 */
public record AiCallOptions(
        Double temperature,
        Integer maxOutputTokens,
        Duration timeout) {
}
