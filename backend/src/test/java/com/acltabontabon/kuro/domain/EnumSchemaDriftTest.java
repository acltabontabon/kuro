package com.acltabontabon.kuro.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Drift tripwire (#12): each enum's wire-string set must equal the member
 * list of the corresponding {@code packages/schemas/src} enum, copied here
 * verbatim. A schema change without a matching domain change fails this test.
 */
class EnumSchemaDriftTest {

    private static <E extends Enum<E> & WireEnum> Set<String> wires(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(WireEnum::wire).collect(Collectors.toSet());
    }

    @Test
    void enumWireValuesMatchSchema() {
        assertThat(wires(DecisionCategory.class))
                .containsExactlyInAnyOrder("employment_intelligence", "rental_intelligence");
        assertThat(wires(SubjectKind.class))
                .containsExactlyInAnyOrder("employer", "rental", "product", "service", "location", "role", "other");
        assertThat(wires(Sentiment.class))
                .containsExactlyInAnyOrder("positive", "negative", "neutral", "mixed");
        assertThat(wires(DataSufficiency.class))
                .containsExactlyInAnyOrder("sufficient", "partial", "insufficient", "unsupported_category");
        assertThat(wires(ResultConfidenceRating.class))
                .containsExactlyInAnyOrder("low", "medium", "high", "unknown");
        assertThat(wires(SubResultConfidenceRating.class))
                .containsExactlyInAnyOrder("low", "medium", "high");
        assertThat(wires(TrustTier.class))
                .containsExactlyInAnyOrder("primary", "secondary", "community", "low_context", "unknown");
        assertThat(wires(SourceType.class))
                .containsExactlyInAnyOrder("review_site", "forum", "social_media", "blog", "news",
                        "company_site", "job_board", "documentation", "other");
        assertThat(wires(LocatorKind.class))
                .containsExactlyInAnyOrder("charRange", "lineRange", "anchor");
        assertThat(wires(ExtractionMethod.class))
                .containsExactlyInAnyOrder("verbatim", "normalized", "synthesized");
        assertThat(wires(SourceTrust.class))
                .containsExactlyInAnyOrder("low", "medium", "high");
        assertThat(wires(AccessedVia.class))
                .containsExactlyInAnyOrder("direct_fetch", "user_paste", "file_upload", "api_import", "other");
        assertThat(wires(RedactionCategory.class))
                .containsExactlyInAnyOrder("pii", "private_id", "email", "real_name", "hidden_metadata", "other");
        assertThat(wires(RequestStatus.class))
                .containsExactlyInAnyOrder("CREATED", "COLLECTING", "EXTRACTING", "SYNTHESIZING", "READY", "FAILED");
        assertThat(wires(ConfidenceDriver.class))
                .containsExactlyInAnyOrder("sourceCount", "sourceDiversity", "sourceFreshness",
                        "signalConsistency", "clarity", "languageAmbiguity", "directnessOfSupport",
                        "themeSupportAggregate", "topicBreadth");
        assertThat(wires(ConfidenceEffect.class))
                .containsExactlyInAnyOrder("raises", "lowers", "neutral");
        assertThat(wires(InsufficientDataReasonKind.class))
                .containsExactlyInAnyOrder("no_sources_found", "no_usable_evidence", "subject_unidentifiable",
                        "out_of_window", "other");
        assertThat(wires(SourceCoverageAssessment.class))
                .containsExactlyInAnyOrder("spam", "duplicate", "inaccessible", "unrelated", "too_vague",
                        "not_about_subject", "stale", "promotional", "other");
    }

    @Test
    void decisionCategoryMapsToItsSubjectKind() {
        assertThat(DecisionCategory.EMPLOYMENT_INTELLIGENCE.subjectKind()).isEqualTo(SubjectKind.EMPLOYER);
        assertThat(DecisionCategory.RENTAL_INTELLIGENCE.subjectKind()).isEqualTo(SubjectKind.RENTAL);
    }

    @Test
    void fromWireRoundTripsAndRejectsUnknownValues() {
        assertThat(WireEnum.fromWire(LocatorKind.class, "charRange")).isEqualTo(LocatorKind.CHAR_RANGE);
        assertThatThrownBy(() -> WireEnum.fromWire(Sentiment.class, "ecstatic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sentiment");
    }
}
