package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.RequestSummary;
import com.acltabontabon.kuro.domain.Subject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storage primitives for the request lifecycle (#13/#14): the kuro_request row,
 * its subject, source-staging rows, and the append-only transition history.
 * Orchestration (which transition is legal, when to refuse) lives in the
 * application layer; this class only reads and writes.
 */
@Service
public class RequestPersistence {

    private final KuroRequestRepository requests;
    private final SubjectRepository subjects;
    private final RequestStatusTransitionRepository transitions;
    private final RequestSourceRepository sources;

    @PersistenceContext
    private EntityManager entityManager;

    RequestPersistence(KuroRequestRepository requests, SubjectRepository subjects,
            RequestStatusTransitionRepository transitions, RequestSourceRepository sources) {
        this.requests = requests;
        this.subjects = subjects;
        this.transitions = transitions;
        this.sources = sources;
    }

    /**
     * Resolves the subject to a persisted id without ever mutating an existing
     * subject (this ticket performs no subject updates): reuse if the given id
     * exists, insert if a given id is missing, generate a new id if absent.
     */
    @Transactional
    public String resolveSubject(Subject subject) {
        String id = subject.id();
        if (id != null && subjects.existsById(id)) {
            return id;
        }
        var entity = KuroResultMapper.toEntity(subject);
        if (id == null) {
            entity.id = KuroResultMapper.newId();
        }
        subjects.save(entity);
        return entity.id;
    }

    @Transactional
    public String insertRequest(DecisionCategory category, String subjectId) {
        var entity = new KuroRequestEntity();
        entity.id = KuroResultMapper.newId();
        entity.status = RequestStatus.CREATED;
        entity.category = category;
        entity.subjectId = subjectId;
        requests.save(entity);
        return entity.id;
    }

    @Transactional(readOnly = true)
    public Optional<RequestStatus> currentStatus(String requestId) {
        return requests.findById(requestId).map(r -> r.status);
    }

    @Transactional(readOnly = true)
    public Optional<RequestSummary> findRequest(String requestId) {
        return requests.findById(requestId).map(this::toSummary);
    }

    @Transactional
    public void updateStatus(String requestId, RequestStatus status) {
        var entity = requests.findById(requestId).orElseThrow(() -> new IllegalStateException(
                "Request vanished mid-transaction: " + requestId));
        entity.status = status;
        requests.save(entity);
    }

    @Transactional
    public void appendTransition(String requestId, RequestStatus from, RequestStatus to, String note) {
        var entity = new RequestStatusTransitionEntity();
        entity.id = KuroResultMapper.newId();
        entity.requestId = requestId;
        entity.fromStatus = from;
        entity.toStatus = to;
        entity.at = KuroResultMapper.iso(OffsetDateTime.now(ZoneOffset.UTC));
        entity.note = note;
        transitions.save(entity);
    }

    @Transactional(readOnly = true)
    public List<RequestStatus> transitionTargets(String requestId) {
        return transitions.findByRequestIdOrderByCreatedAt(requestId).stream().map(t -> t.toStatus).toList();
    }

    @Transactional
    public void insertSource(String requestId, String kind, String value) {
        var entity = new RequestSourceEntity();
        entity.id = KuroResultMapper.newId();
        entity.requestId = requestId;
        entity.kind = kind;
        entity.value = value;
        sources.save(entity);
    }

    @Transactional(readOnly = true)
    public List<RequestSummary> listRequests(int limit, int offset) {
        return entityManager
                .createQuery("select r from KuroRequestEntity r order by r.createdAt desc", KuroRequestEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private RequestSummary toSummary(KuroRequestEntity entity) {
        String displayName = entity.subjectId == null ? null
                : subjects.findById(entity.subjectId).map(s -> s.displayName).orElse(null);
        return new RequestSummary(entity.id, entity.status, entity.category, entity.subjectId, displayName,
                KuroResultMapper.ts(entity.createdAt));
    }
}
