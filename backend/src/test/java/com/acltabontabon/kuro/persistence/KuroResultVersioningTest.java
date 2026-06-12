package com.acltabontabon.kuro.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.ResultAlreadyPersistedException;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class KuroResultVersioningTest {

    @Autowired
    private KuroResultPersistence persistence;

    @Autowired
    private KuroRequestRepository requests;

    @Autowired
    private KuroResultRepository results;

    @PersistenceContext
    private EntityManager entityManager;

    private String newRequest() {
        var request = new KuroRequestEntity();
        request.id = KuroResultMapper.newId();
        request.status = RequestStatus.CREATED;
        return requests.save(request).id;
    }

    private KuroResult refusal(String id) {
        return new KuroResult.UnsupportedCategory(id,
                new Subject("subject-x", SubjectKind.OTHER, "Cedar Valley", null),
                OffsetDateTime.parse("2026-06-11T16:00:00Z"), null, "healthcare",
                List.of(DecisionCategory.EMPLOYMENT_INTELLIGENCE, DecisionCategory.RENTAL_INTELLIGENCE),
                "scope refusal");
    }

    @Test
    void rerunCreatesNewVersionAndFlipsCurrent() {
        String requestId = newRequest();
        persistence.saveNewVersion(refusal("res-1"), requestId);
        persistence.saveNewVersion(refusal("res-2"), requestId);
        entityManager.flush();
        entityManager.clear();

        assertThat(persistence.loadCurrent(requestId)).get().extracting(KuroResult::id).isEqualTo("res-2");

        var v1 = results.findById("res-1").orElseThrow();
        var v2 = results.findById("res-2").orElseThrow();
        assertThat(v1.version).isEqualTo(1);
        assertThat(v1.isCurrent).isFalse();
        assertThat(v2.version).isEqualTo(2);
        assertThat(v2.isCurrent).isTrue();
        assertThat(persistence.load("res-1")).isPresent();
    }

    @Test
    void overwritingAnExistingResultIsRejected() {
        String requestId = newRequest();
        persistence.save(refusal("res-dup"), requestId, 1, true);

        assertThatThrownBy(() -> persistence.save(refusal("res-dup"), requestId, 2, false))
                .isInstanceOf(ResultAlreadyPersistedException.class);
    }
}
