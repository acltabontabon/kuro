package com.acltabontabon.kuro.application;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.IllegalTransitionException;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.RequestLifecycle;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.persistence.KuroResultPersistence;
import com.acltabontabon.kuro.persistence.RequestPersistence;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side workflows for requests (#13/#14/#15). Each method is one
 * transaction, so a status change and its transition row are always persisted
 * together. Transition legality comes from {@link RequestLifecycle}.
 */
@Service
public class RequestCommandService {

    /** The MVP supported categories, in the order the schema lists them. */
    public static final List<DecisionCategory> SUPPORTED_CATEGORIES =
            List.of(DecisionCategory.EMPLOYMENT_INTELLIGENCE, DecisionCategory.RENTAL_INTELLIGENCE);

    private final RequestPersistence requests;
    private final KuroResultPersistence results;

    RequestCommandService(RequestPersistence requests, KuroResultPersistence results) {
        this.requests = requests;
        this.results = results;
    }

    /** Creates a CREATED request for a supported category and returns its id. */
    @Transactional
    public String createSupportedRequest(DecisionCategory category, Subject subject) {
        String subjectId = requests.resolveSubject(subject);
        return requests.insertRequest(category, subjectId);
    }

    /**
     * Handles an out-of-scope category as the product's structured refusal, not
     * an error: creates the request, persists a synchronous {@code
     * unsupported_category} result as v1/current, and lands the request READY via
     * the reserved refusal edge. Returns the result for a 200 response.
     */
    @Transactional
    public KuroResult refuseUnsupportedCategory(String requestedCategory, Subject subject) {
        String subjectId = requests.resolveSubject(subject);
        String requestId = requests.insertRequest(null, subjectId);
        var resolvedSubject = new Subject(subjectId, subject.kind(), subject.displayName(), subject.description());
        var result = new KuroResult.UnsupportedCategory(
                UUID.randomUUID().toString(),
                resolvedSubject,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                requestedCategory,
                SUPPORTED_CATEGORIES,
                refusalMessage(requestedCategory));
        results.saveNewVersion(result, requestId);

        if (!RequestLifecycle.isUnsupportedCategoryRefusalLanding(RequestStatus.CREATED, RequestStatus.READY)) {
            throw new IllegalStateException("Refusal landing edge is no longer reserved");
        }
        requests.updateStatus(requestId, RequestStatus.READY);
        requests.appendTransition(requestId, RequestStatus.CREATED, RequestStatus.READY,
                "unsupported_category refusal");
        return result;
    }

    /** Stages a user-attached source (exactly one of url/text) on a request. */
    @Transactional
    public void addSource(String requestId, String url, String text) {
        boolean hasUrl = url != null && !url.isBlank();
        boolean hasText = text != null && !text.isBlank();
        if (hasUrl == hasText) {
            throw new IllegalArgumentException("Provide exactly one of 'url' or 'text'");
        }
        if (requests.currentStatus(requestId).isEmpty()) {
            throw new RequestNotFoundException(requestId);
        }
        requests.insertSource(requestId, hasUrl ? "url" : "text", hasUrl ? url : text);
    }

    /** Advances a request along a legal lifecycle edge, recording the transition. */
    @Transactional
    public void transitionTo(String requestId, RequestStatus target, String note) {
        RequestStatus from = requests.currentStatus(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        if (!RequestLifecycle.canTransition(from, target)) {
            throw new IllegalTransitionException(from, target);
        }
        requests.updateStatus(requestId, target);
        requests.appendTransition(requestId, from, target, note);
    }

    private static String refusalMessage(String requestedCategory) {
        return "KURO does not evaluate '" + requestedCategory + "'. KURO's MVP scope is limited to "
                + "employment_intelligence and rental_intelligence. This is a scope decision, not an "
                + "assessment of the subject: KURO did not gather or interpret any community feedback for it.";
    }
}
