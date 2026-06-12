package com.acltabontabon.kuro.domain;

import java.util.List;

// mirrors packages/schemas/src/confidence.ts (level "result").
// supportScore and inputs are @internal diagnostics: persisted and carried
// here, but stripped from API responses at the DTO boundary (#13).
public record ResultConfidence(
        ResultConfidenceRating rating,
        Double supportScore,
        Inputs inputs,
        List<ConfidenceReason> reasons) {

    public ResultConfidence {
        reasons = List.copyOf(reasons);
    }

    public record Inputs(
            Integer sourceCount,
            Double sourceDiversity,
            Double sourceFreshness,
            Double signalConsistency,
            Double themeSupportAggregate,
            Double topicBreadth) {
    }
}
