package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.AiPhase;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per AI model invocation (#16). Append-only, written alongside the
 * result version it describes so the audit trail cannot silently diverge from
 * the result. Holds the prompt <em>version</em> and token counts only — never
 * the raw prompt text (PII posture).
 */
@Entity
@Table(name = "ai_run")
class AiRunEntity extends BaseEntity {

    String requestId;
    String resultId;
    AiPhase phase;
    String modelId;
    String promptVersion;
    Integer inputTokens;
    Integer outputTokens;
    String startedAt;
    String finishedAt;
}
