package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.AiRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write primitive for the AI audit log (#16). A single transactional insert
 * callers invoke inside the same transaction as the result-version write they
 * describe, so a result can never exist without its provenance. Orchestration
 * (which phases ran, with what tokens) lives in the extraction/synthesis layer;
 * this class only stores what it is handed.
 */
@Service
public class AiRunPersistence {

    private final AiRunRepository aiRuns;

    AiRunPersistence(AiRunRepository aiRuns) {
        this.aiRuns = aiRuns;
    }

    @Transactional
    public String record(AiRun run, String requestId, String resultId) {
        var entity = new AiRunEntity();
        entity.id = KuroResultMapper.newId();
        entity.requestId = requestId;
        entity.resultId = resultId;
        entity.phase = run.phase();
        entity.modelId = run.modelId();
        entity.promptVersion = run.promptVersion();
        entity.inputTokens = run.inputTokens();
        entity.outputTokens = run.outputTokens();
        entity.startedAt = KuroResultMapper.iso(run.startedAt());
        entity.finishedAt = KuroResultMapper.iso(run.finishedAt());
        aiRuns.save(entity);
        return entity.id;
    }
}
