package com.acltabontabon.kuro.application;

import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Provenance;
import com.acltabontabon.kuro.domain.RequestSummary;
import com.acltabontabon.kuro.domain.SourceAttribution;
import com.acltabontabon.kuro.persistence.KuroResultPersistence;
import com.acltabontabon.kuro.persistence.RequestPersistence;
import com.acltabontabon.kuro.persistence.ResultEvidenceReadStore;
import com.acltabontabon.kuro.persistence.TraceabilityReadStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Read-side queries for the API (#13), so controllers never touch persistence
 * directly. Returns {@code Optional.empty()} where the API answers 404/409.
 */
@Service
public class RequestQueryService {

    private final RequestPersistence requests;
    private final KuroResultPersistence results;
    private final ResultEvidenceReadStore evidence;
    private final TraceabilityReadStore traceability;

    RequestQueryService(RequestPersistence requests, KuroResultPersistence results,
            ResultEvidenceReadStore evidence, TraceabilityReadStore traceability) {
        this.requests = requests;
        this.results = results;
        this.evidence = evidence;
        this.traceability = traceability;
    }

    public Optional<RequestSummary> getRequest(String requestId) {
        return requests.findRequest(requestId);
    }

    public List<RequestSummary> listRequests(int limit, int offset) {
        return requests.listRequests(limit, offset);
    }

    /** The current result, or empty when none is ready yet (api 409). */
    public Optional<KuroResult> getCurrentResult(String requestId) {
        return results.loadCurrent(requestId);
    }

    /**
     * The evidence chain of the current result, or empty when none is ready yet
     * (api 409). A present-but-empty list means the current result carries no
     * evidence (e.g. an {@code unsupported_category} refusal → 200 []).
     */
    public Optional<List<EvidenceView>> getEvidence(String requestId) {
        return evidence.currentChain(requestId).map(chain -> {
            Map<String, SourceAttribution> bySource = chain.attributions().stream()
                    .collect(Collectors.toMap(SourceAttribution::sourceDocumentId, Function.identity(),
                            (first, second) -> first));
            return chain.evidence().stream()
                    .map(item -> new EvidenceView(item, bySource.get(item.sourceDocumentId())))
                    .toList();
        });
    }

    /**
     * Full provenance of the current result version (#16), or empty when none is
     * ready yet (api 409): signals → evidence → redacted attributions, plus the
     * AI runs that produced it.
     */
    public Optional<Provenance> getProvenance(String requestId) {
        return traceability.provenanceForCurrent(requestId);
    }
}
