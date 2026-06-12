package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mirrors packages/schemas/src/result.ts: a discriminated union on
 * dataSufficiency, so illegal shapes per status are unrepresentable.
 * Conditional product rules (confidence thresholds, referential integrity,
 * cardinalities) live in the validation layer (#15), not here.
 */
public sealed interface KuroResult {

    String id();

    Subject subject();

    OffsetDateTime generatedAt();

    /** Optional on every variant. */
    List<String> limitations();

    DataSufficiency dataSufficiency();

    record Sufficient(
            String id,
            Subject subject,
            OffsetDateTime generatedAt,
            List<String> limitations,
            DecisionCategory category,
            String summary,
            List<SourceDocument> sourceDocuments,
            List<SourceAttribution> sourceAttributions,
            List<Evidence> evidence,
            List<Signal> signals,
            List<Theme> themes,
            KuroInference inference,
            SourceSummary sourceSummary,
            ResultConfidence confidence,
            String finalKuro) implements KuroResult {

        public Sufficient {
            limitations = limitations == null ? null : List.copyOf(limitations);
            sourceDocuments = List.copyOf(sourceDocuments);
            sourceAttributions = List.copyOf(sourceAttributions);
            evidence = List.copyOf(evidence);
            signals = List.copyOf(signals);
            themes = List.copyOf(themes);
        }

        @Override
        public DataSufficiency dataSufficiency() {
            return DataSufficiency.SUFFICIENT;
        }
    }

    record Partial(
            String id,
            Subject subject,
            OffsetDateTime generatedAt,
            List<String> limitations,
            DecisionCategory category,
            String summary,
            List<SourceDocument> sourceDocuments,
            List<SourceAttribution> sourceAttributions,
            List<Evidence> evidence,
            List<Signal> signals,
            List<Theme> themes,
            KuroInference inference,
            SourceSummary sourceSummary,
            ResultConfidence confidence,
            String finalKuro,
            List<EvidenceGap> evidenceGaps) implements KuroResult {

        public Partial {
            limitations = limitations == null ? null : List.copyOf(limitations);
            sourceDocuments = List.copyOf(sourceDocuments);
            sourceAttributions = List.copyOf(sourceAttributions);
            evidence = List.copyOf(evidence);
            signals = List.copyOf(signals);
            themes = List.copyOf(themes);
            evidenceGaps = List.copyOf(evidenceGaps);
        }

        @Override
        public DataSufficiency dataSufficiency() {
            return DataSufficiency.PARTIAL;
        }
    }

    record Insufficient(
            String id,
            Subject subject,
            OffsetDateTime generatedAt,
            List<String> limitations,
            DecisionCategory category,
            String summary,
            List<SourceDocument> sourceDocuments,
            List<SourceAttribution> sourceAttributions,
            SourceSummary sourceSummary,
            List<SourceCoverageEntry> sourceCoverage,
            InsufficientDataReason insufficientDataReason,
            List<SuggestedNextSource> suggestedNextSources,
            ResultConfidence confidence,
            String finalKuro) implements KuroResult {

        public Insufficient {
            limitations = limitations == null ? null : List.copyOf(limitations);
            sourceDocuments = List.copyOf(sourceDocuments);
            sourceAttributions = List.copyOf(sourceAttributions);
            sourceCoverage = sourceCoverage == null ? null : List.copyOf(sourceCoverage);
            suggestedNextSources = List.copyOf(suggestedNextSources);
        }

        @Override
        public DataSufficiency dataSufficiency() {
            return DataSufficiency.INSUFFICIENT;
        }
    }

    record UnsupportedCategory(
            String id,
            Subject subject,
            OffsetDateTime generatedAt,
            List<String> limitations,
            String requestedCategory,
            List<DecisionCategory> supportedCategories,
            String refusalMessage) implements KuroResult {

        public UnsupportedCategory {
            limitations = limitations == null ? null : List.copyOf(limitations);
            supportedCategories = List.copyOf(supportedCategories);
        }

        @Override
        public DataSufficiency dataSufficiency() {
            return DataSufficiency.UNSUPPORTED_CATEGORY;
        }
    }

    record EvidenceGap(String topic, String note) {
    }

    record InsufficientDataReason(InsufficientDataReasonKind kind, String explanation) {
    }

    record SuggestedNextSource(String sourceType, String rationale) {
    }

    record SourceCoverageEntry(String sourceDocumentId, SourceCoverageAssessment assessment, String note) {
    }
}
