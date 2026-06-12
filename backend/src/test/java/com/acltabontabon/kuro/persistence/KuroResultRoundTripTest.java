package com.acltabontabon.kuro.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acltabontabon.kuro.domain.AccessedVia;
import com.acltabontabon.kuro.domain.ConfidenceDriver;
import com.acltabontabon.kuro.domain.ConfidenceEffect;
import com.acltabontabon.kuro.domain.ConfidenceReason;
import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.InsufficientDataReasonKind;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.RedactionCategory;
import com.acltabontabon.kuro.domain.RedactionRecord;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.ResultConfidence;
import com.acltabontabon.kuro.domain.ResultConfidenceRating;
import com.acltabontabon.kuro.domain.SourceAttribution;
import com.acltabontabon.kuro.domain.SourceCoverageAssessment;
import com.acltabontabon.kuro.domain.SourceDocument;
import com.acltabontabon.kuro.domain.SourceType;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.domain.TrustTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptance tests for #11: the schema example fixture round-trips
 * entity-ward and back without field loss (including @internal confidence
 * supportScore/inputs), and database FKs are live.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class KuroResultRoundTripTest {

    private static final Path EMPLOYMENT_FIXTURE =
            Path.of("../packages/schemas/examples/result.employment.json");

    @Autowired
    private KuroResultPersistence persistence;

    @Autowired
    private KuroRequestRepository requests;

    @Autowired
    private SignalEvidenceRepository signalEvidence;

    @PersistenceContext
    private EntityManager entityManager;

    private String newRequest() {
        var request = new KuroRequestEntity();
        request.id = KuroResultMapper.newId();
        request.status = RequestStatus.CREATED;
        return requests.save(request).id;
    }

    private KuroResult saveAndReload(KuroResult result) {
        persistence.save(result, newRequest(), 1, true);
        entityManager.flush();
        entityManager.clear();
        return persistence.load(result.id()).orElseThrow();
    }

    @Test
    void roundTripsTheSufficientEmploymentFixture() throws IOException {
        KuroResult original = FixtureLoader.load(EMPLOYMENT_FIXTURE);
        assertThat(saveAndReload(original)).isEqualTo(original);
    }

    @Test
    void roundTripsAPartialResult() throws IOException {
        var sufficient = (KuroResult.Sufficient) FixtureLoader.load(EMPLOYMENT_FIXTURE);
        var original = new KuroResult.Partial("result_partial", sufficient.subject(), sufficient.generatedAt(),
                sufficient.limitations(), sufficient.category(), sufficient.summary(),
                sufficient.sourceDocuments(), sufficient.sourceAttributions(), sufficient.evidence(),
                sufficient.signals(), sufficient.themes(), sufficient.inference(), sufficient.sourceSummary(),
                sufficient.confidence(), sufficient.finalKuro(),
                List.of(new KuroResult.EvidenceGap("Compensation", "No salary discussion found.")));
        assertThat(saveAndReload(original)).isEqualTo(original);
    }

    @Test
    void roundTripsAnInsufficientResultWithCoverageAndRedactions() {
        var subject = new Subject("subject_ins", SubjectKind.EMPLOYER, "Obscure Holdings LLC", null);
        var document = new SourceDocument("src_ins_1", "https://example.com/post", "blog", null,
                OffsetDateTime.parse("2026-01-02T00:00:00Z"), OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                null, null, null);
        var attribution = new SourceAttribution("att_ins_1", "src_ins_1", SourceType.BLOG,
                "https://example.com/post", null, "Old post", null,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"), OffsetDateTime.parse("2026-01-02T00:00:00Z"),
                AccessedVia.DIRECT_FETCH, TrustTier.UNKNOWN, "Unverified blog without authorship.",
                Map.of("lang", "en"),
                List.of(new RedactionRecord("author", RedactionCategory.REAL_NAME, "Real name removed.")));
        var original = new KuroResult.Insufficient("result_ins", subject,
                OffsetDateTime.parse("2026-06-01T00:00:00Z"), null,
                DecisionCategory.EMPLOYMENT_INTELLIGENCE, "Too little usable data.",
                List.of(document), List.of(attribution), null,
                List.of(new KuroResult.SourceCoverageEntry("src_ins_1", SourceCoverageAssessment.STALE,
                        "Post predates the review window.")),
                new KuroResult.InsufficientDataReason(InsufficientDataReasonKind.NO_USABLE_EVIDENCE,
                        "No first-hand experiences found."),
                List.of(new KuroResult.SuggestedNextSource("verified employee reviews",
                        "First-hand accounts would raise coverage.")),
                new ResultConfidence(ResultConfidenceRating.UNKNOWN, null,
                        new ResultConfidence.Inputs(null, null, null, null, null, null),
                        List.of(new ConfidenceReason(ConfidenceDriver.SOURCE_COUNT, ConfidenceEffect.LOWERS,
                                "No usable evidence."))),
                "KURO cannot describe community sentiment for Obscure Holdings LLC yet.");
        assertThat(saveAndReload(original)).isEqualTo(original);
    }

    @Test
    void roundTripsAnUnsupportedCategoryResult() {
        var subject = new Subject("subject_unsup", SubjectKind.OTHER, "Some Clinic", null);
        var original = new KuroResult.UnsupportedCategory("result_unsup", subject,
                OffsetDateTime.parse("2026-06-01T00:00:00Z"), List.of("Out of MVP scope."),
                "medical_advice", List.of(DecisionCategory.EMPLOYMENT_INTELLIGENCE,
                        DecisionCategory.RENTAL_INTELLIGENCE),
                "KURO does not support this decision category.");
        assertThat(saveAndReload(original)).isEqualTo(original);
    }

    @Test
    void rejectsSignalEvidenceReferencingMissingEvidence() throws IOException {
        KuroResult original = FixtureLoader.load(EMPLOYMENT_FIXTURE);
        persistence.save(original, newRequest(), 1, true);
        entityManager.flush();

        var join = new SignalEvidenceEntity();
        join.id = KuroResultMapper.newId();
        join.signalId = "sig_min_wlb";
        join.evidenceId = "ev_does_not_exist";
        join.ordinal = 1;
        assertThatThrownBy(() -> signalEvidence.saveAndFlush(join))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("FOREIGN KEY");
    }
}
