package com.acltabontabon.kuro.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.persistence.FixtureLoader;
import com.acltabontabon.kuro.persistence.KuroResultPersistence;
import com.acltabontabon.kuro.persistence.RequestPersistence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class RequestQueryServiceTest {

    private static final Path EMPLOYMENT = Path.of("../packages/schemas/examples/result.employment.json");

    @Autowired
    private RequestQueryService queries;

    @Autowired
    private RequestPersistence requests;

    @Autowired
    private KuroResultPersistence results;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void evidenceForASufficientResultLinksRedactedAttributions() throws IOException {
        String requestId = requests.insertRequest(DecisionCategory.EMPLOYMENT_INTELLIGENCE, null);
        KuroResult sufficient = FixtureLoader.load(EMPLOYMENT);
        results.saveNewVersion(sufficient, requestId);
        entityManager.flush();
        entityManager.clear();

        List<EvidenceView> chain = queries.getEvidence(requestId).orElseThrow();

        assertThat(chain).isNotEmpty();
        // EvidenceView carries only Evidence + SourceAttribution by type — no raw
        // SourceDocument author/content can be reached. Every item links a source.
        assertThat(chain).allSatisfy(view -> assertThat(view.source()).isNotNull());
        assertThat(queries.getCurrentResult(requestId)).get().extracting(KuroResult::id)
                .isEqualTo(sufficient.id());
    }

    @Test
    void evidenceForARefusalIsAPresentEmptyList() {
        String requestId = requests.insertRequest(null, null);
        results.saveNewVersion(refusal("res-refusal"), requestId);
        entityManager.flush();
        entityManager.clear();

        assertThat(queries.getEvidence(requestId)).get().asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.list(EvidenceView.class)).isEmpty();
    }

    @Test
    void evidenceWithoutACurrentResultIsEmptyOptional() {
        String requestId = requests.insertRequest(null, null);

        assertThat(queries.getEvidence(requestId)).isEmpty();
        assertThat(queries.getCurrentResult(requestId)).isEmpty();
    }

    private KuroResult refusal(String id) {
        return new KuroResult.UnsupportedCategory(id,
                new Subject("subject-refusal", SubjectKind.OTHER, "Cedar Valley", null),
                OffsetDateTime.parse("2026-06-11T16:00:00Z"), null, "healthcare",
                List.of(DecisionCategory.EMPLOYMENT_INTELLIGENCE, DecisionCategory.RENTAL_INTELLIGENCE),
                "scope refusal");
    }
}
