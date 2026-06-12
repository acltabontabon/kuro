package com.acltabontabon.kuro.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.IllegalTransitionException;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.RequestSummary;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.persistence.RequestPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class RequestCommandServiceTest {

    @Autowired
    private RequestCommandService commands;

    @Autowired
    private RequestQueryService queries;

    @Autowired
    private RequestPersistence requests;

    private Subject subject() {
        return new Subject(null, SubjectKind.EMPLOYER, "Acme Corp", null);
    }

    @Test
    void advancesThroughTheFullLifecycleRecordingEveryTransitionInOrder() {
        String id = commands.createSupportedRequest(DecisionCategory.EMPLOYMENT_INTELLIGENCE, subject());

        commands.transitionTo(id, RequestStatus.COLLECTING, null);
        commands.transitionTo(id, RequestStatus.EXTRACTING, null);
        commands.transitionTo(id, RequestStatus.SYNTHESIZING, "synthesizing now");
        commands.transitionTo(id, RequestStatus.READY, null);

        assertThat(queries.getRequest(id)).get().extracting(RequestSummary::status)
                .isEqualTo(RequestStatus.READY);
        assertThat(requests.transitionTargets(id)).containsExactly(
                RequestStatus.COLLECTING, RequestStatus.EXTRACTING, RequestStatus.SYNTHESIZING, RequestStatus.READY);
    }

    @Test
    void rejectsAnIllegalTransition() {
        String id = commands.createSupportedRequest(DecisionCategory.RENTAL_INTELLIGENCE, subject());

        assertThatThrownBy(() -> commands.transitionTo(id, RequestStatus.READY, null))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void refusalProducesAnUnsupportedCategoryResultAndLandsReady() {
        KuroResult result = commands.refuseUnsupportedCategory("healthcare", subject());

        assertThat(result).isInstanceOfSatisfying(KuroResult.UnsupportedCategory.class, refusal -> {
            assertThat(refusal.requestedCategory()).isEqualTo("healthcare");
            assertThat(refusal.supportedCategories()).containsExactly(
                    DecisionCategory.EMPLOYMENT_INTELLIGENCE, DecisionCategory.RENTAL_INTELLIGENCE);
        });
    }

    @Test
    void addSourceRequiresExactlyOneOfUrlOrTextAndAKnownRequest() {
        String id = commands.createSupportedRequest(DecisionCategory.EMPLOYMENT_INTELLIGENCE, subject());

        assertThatThrownBy(() -> commands.addSource(id, "https://x", "also text"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> commands.addSource(id, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> commands.addSource("missing", "https://x", null))
                .isInstanceOf(RequestNotFoundException.class);

        commands.addSource(id, "https://example.com/review", null);
    }
}
