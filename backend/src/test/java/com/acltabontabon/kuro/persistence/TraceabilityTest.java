package com.acltabontabon.kuro.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acltabontabon.kuro.domain.AiPhase;
import com.acltabontabon.kuro.domain.AiRun;
import com.acltabontabon.kuro.domain.Evidence;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Provenance;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.Signal;
import com.acltabontabon.kuro.domain.SourceAttribution;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptance tests for #16. The audit trail is only worth storing if a result
 * can actually be reconstructed from it, so these walk the persisted chain for a
 * real result (orphan-detection) and round-trip an ai_run through the provenance
 * read path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class TraceabilityTest {

    private static final Path EMPLOYMENT_FIXTURE =
            Path.of("../packages/schemas/examples/result.employment.json");

    @Autowired
    private KuroResultPersistence persistence;

    @Autowired
    private AiRunPersistence aiRuns;

    @Autowired
    private TraceabilityReadStore traceability;

    @Autowired
    private KuroRequestRepository requests;

    @PersistenceContext
    private EntityManager entityManager;

    private String newRequest() {
        var request = new KuroRequestEntity();
        request.id = KuroResultMapper.newId();
        request.status = RequestStatus.CREATED;
        return requests.save(request).id;
    }

    @Test
    void everySignalResolvesToEvidenceAndEverySourceToAnAttribution() throws IOException {
        KuroResult result = FixtureLoader.load(EMPLOYMENT_FIXTURE);
        String requestId = newRequest();
        persistence.save(result, requestId, 1, true);
        entityManager.flush();
        entityManager.clear();

        Provenance provenance = traceability.provenanceForCurrent(requestId).orElseThrow();

        // The walk is only meaningful if there is something to walk.
        assertThat(provenance.signals()).isNotEmpty();
        assertThat(provenance.evidence()).isNotEmpty();

        Set<String> evidenceIds = provenance.evidence().stream().map(Evidence::id).collect(Collectors.toSet());
        // Signal -> Evidence: no signal cites evidence that does not exist.
        for (Signal signal : provenance.signals()) {
            assertThat(evidenceIds).as("evidence for signal %s", signal.id()).containsAll(signal.evidenceIds());
        }

        Set<String> attributedDocs = provenance.attributions().stream()
                .map(SourceAttribution::sourceDocumentId).collect(Collectors.toSet());
        // Evidence -> SourceDocument -> SourceAttribution: every cited source is attributed.
        for (Evidence evidence : provenance.evidence()) {
            assertThat(attributedDocs).as("attribution for source %s", evidence.sourceDocumentId())
                    .contains(evidence.sourceDocumentId());
        }
    }

    @Test
    void recordsAndReadsBackAnAiRunForTheResultVersion() throws IOException {
        KuroResult result = FixtureLoader.load(EMPLOYMENT_FIXTURE);
        String requestId = newRequest();
        persistence.save(result, requestId, 1, true);

        var run = new AiRun(AiPhase.EXTRACTION, "claude-opus-4-8", "extraction-v1", 1200, 340,
                OffsetDateTime.parse("2026-06-01T00:00:00Z"), OffsetDateTime.parse("2026-06-01T00:00:05Z"));
        aiRuns.record(run, requestId, result.id());
        entityManager.flush();
        entityManager.clear();

        Provenance provenance = traceability.provenanceForCurrent(requestId).orElseThrow();

        assertThat(provenance.resultId()).isEqualTo(result.id());
        assertThat(provenance.version()).isEqualTo(1);
        assertThat(provenance.aiRuns()).containsExactly(run);
    }
}
