package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;

/**
 * One AI model invocation that contributed to a result version (issue #16):
 * which {@link AiPhase}, which model, which prompt version, and its token usage.
 * The prompt <em>version</em> is recorded, never the raw prompt text — the
 * prompt carries source PII, the identifier does not. {@code inputTokens} /
 * {@code outputTokens} and {@code finishedAt} are nullable (a run may be
 * recorded before completion or where token counts are unavailable).
 */
public record AiRun(
        AiPhase phase,
        String modelId,
        String promptVersion,
        Integer inputTokens,
        Integer outputTokens,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {
}
