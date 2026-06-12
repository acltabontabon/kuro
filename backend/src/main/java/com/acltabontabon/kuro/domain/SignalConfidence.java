package com.acltabontabon.kuro.domain;

import java.util.List;

// mirrors packages/schemas/src/confidence.ts (level "signal").
// supportScore and inputs are @internal diagnostics: persisted and carried
// here, but stripped from API responses at the DTO boundary (#13).
public record SignalConfidence(
        SubResultConfidenceRating rating,
        Double supportScore,
        Inputs inputs,
        List<ConfidenceReason> reasons) {

    public SignalConfidence {
        reasons = List.copyOf(reasons);
    }

    public record Inputs(
            Integer sourceCount,
            Double sourceDiversity,
            Double sourceFreshness,
            Double signalConsistency,
            Double clarity,
            Double languageAmbiguity,
            Double directnessOfSupport) {
    }
}
