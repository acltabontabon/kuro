package com.acltabontabon.kuro.persistence;

import static java.util.stream.Collectors.groupingBy;

import com.acltabontabon.kuro.domain.DataSufficiency;
import com.acltabontabon.kuro.domain.Evidence;
import com.acltabontabon.kuro.domain.SourceAttribution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side for the evidence-explorer endpoint (#13). Returns the evidence
 * chain of a request's current result as {@link Evidence} linked to redacted
 * {@link SourceAttribution} — never raw {@code source_document} author/content.
 */
@Service
public class ResultEvidenceReadStore {

    private final KuroResultRepository results;
    private final EvidenceRepository evidence;
    private final SourceAttributionRepository attributions;
    private final RedactionRepository redactions;

    ResultEvidenceReadStore(KuroResultRepository results, EvidenceRepository evidence,
            SourceAttributionRepository attributions, RedactionRepository redactions) {
        this.results = results;
        this.evidence = evidence;
        this.attributions = attributions;
        this.redactions = redactions;
    }

    /** The evidence chain of a value is a list of items + their attributions. */
    public record EvidenceChain(List<Evidence> evidence, List<SourceAttribution> attributions) {
    }

    /**
     * The evidence chain of the request's current result. {@code empty} means
     * there is no current result yet (the api answers 409); a present chain with
     * empty lists means the current result carries no evidence (e.g. an
     * {@code unsupported_category} refusal).
     */
    @Transactional(readOnly = true)
    public Optional<EvidenceChain> currentChain(String requestId) {
        return results.findByRequestIdAndIsCurrentTrue(requestId).map(result -> {
            if (result.dataSufficiency == DataSufficiency.UNSUPPORTED_CATEGORY) {
                return new EvidenceChain(List.of(), List.of());
            }
            var evidenceRows = evidence.findByResultIdOrderById(result.id);
            var evidenceItems = evidenceRows.stream().map(KuroResultMapper::toDomain).toList();
            var docIds = evidenceRows.stream().map(e -> e.sourceDocumentId).distinct().toList();
            return new EvidenceChain(evidenceItems, attributionsFor(docIds));
        });
    }

    private List<SourceAttribution> attributionsFor(List<String> sourceDocumentIds) {
        if (sourceDocumentIds.isEmpty()) {
            return List.of();
        }
        var attEntities = attributions.findBySourceDocumentIdInOrderById(sourceDocumentIds);
        Map<String, List<RedactionEntity>> redactionsByAttribution = attEntities.isEmpty() ? Map.of()
                : redactions.findBySourceAttributionIdInOrderById(attEntities.stream().map(a -> a.id).toList())
                        .stream().collect(groupingBy(r -> r.sourceAttributionId));
        return attEntities.stream()
                .map(a -> KuroResultMapper.toDomain(a, redactionsByAttribution.getOrDefault(a.id, List.of())))
                .toList();
    }
}
