package com.acltabontabon.kuro.persistence;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.acltabontabon.kuro.domain.AiRun;
import com.acltabontabon.kuro.domain.Provenance;
import com.acltabontabon.kuro.domain.Signal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side for the provenance/traceability surface (#16): given a request,
 * assembles the complete provenance of its current result version — the
 * structural chain (signals → evidence → redacted attributions, reusing
 * {@link ResultEvidenceReadStore}) plus the {@link AiRun} audit records.
 */
@Service
public class TraceabilityReadStore {

    private final KuroResultRepository results;
    private final SignalRepository signals;
    private final SignalEvidenceRepository signalEvidence;
    private final AiRunRepository aiRuns;
    private final ResultEvidenceReadStore evidenceChain;

    TraceabilityReadStore(KuroResultRepository results, SignalRepository signals,
            SignalEvidenceRepository signalEvidence, AiRunRepository aiRuns,
            ResultEvidenceReadStore evidenceChain) {
        this.results = results;
        this.signals = signals;
        this.signalEvidence = signalEvidence;
        this.aiRuns = aiRuns;
        this.evidenceChain = evidenceChain;
    }

    /**
     * The provenance of the request's current result version, or empty when no
     * current result exists yet (the api answers 409). A present provenance with
     * empty chains means the current result carries none (e.g. an
     * {@code unsupported_category} refusal).
     */
    @Transactional(readOnly = true)
    public Optional<Provenance> provenanceForCurrent(String requestId) {
        return results.findByRequestIdAndIsCurrentTrue(requestId).map(result -> {
            var chain = evidenceChain.currentChain(requestId).orElseThrow();
            return new Provenance(result.id, result.version, loadSignals(result.id),
                    chain.evidence(), chain.attributions(), loadAiRuns(result.id));
        });
    }

    private List<Signal> loadSignals(String resultId) {
        var signalEntities = signals.findByResultIdOrderById(resultId);
        var joins = signalEntities.isEmpty() ? List.<SignalEvidenceEntity>of()
                : signalEvidence.findBySignalIdInOrderByOrdinal(signalEntities.stream().map(s -> s.id).toList());
        Map<String, List<String>> evidenceIdsBySignal = joins.stream()
                .collect(groupingBy(j -> j.signalId, LinkedHashMap::new, mapping(j -> j.evidenceId, toList())));
        return signalEntities.stream()
                .map(s -> KuroResultMapper.toDomain(s, evidenceIdsBySignal.getOrDefault(s.id, List.of())))
                .toList();
    }

    private List<AiRun> loadAiRuns(String resultId) {
        return aiRuns.findByResultIdOrderByStartedAt(resultId).stream().map(KuroResultMapper::toDomain).toList();
    }
}
