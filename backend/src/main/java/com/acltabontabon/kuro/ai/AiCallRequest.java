package com.acltabontabon.kuro.ai;

import com.acltabontabon.kuro.domain.AiPhase;
import java.util.Objects;

/**
 * A single structured-generation request (issue #17). {@code prompt} is the
 * <em>fully rendered</em> prompt string — providers do not template (#20).
 * {@code responseSchema} is a JSON Schema string the output must satisfy (#21).
 * {@code options} may be {@code null} to fall back to configured defaults.
 */
public record AiCallRequest(
        String prompt,
        String promptVersion,
        AiPhase phase,
        String modelId,
        String responseSchema,
        AiCallOptions options) {

    public AiCallRequest {
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(promptVersion, "promptVersion must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
    }
}
