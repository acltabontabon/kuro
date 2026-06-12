package com.acltabontabon.kuro.persistence;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.acltabontabon.kuro.domain.Evidence;
import com.acltabontabon.kuro.domain.KuroInference;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.ResultAlreadyPersistedException;
import com.acltabontabon.kuro.domain.Signal;
import com.acltabontabon.kuro.domain.SourceAttribution;
import com.acltabontabon.kuro.domain.SourceDocument;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.Theme;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The persistence entry point for whole KuroResult graphs. request_id,
 * version and is_current are caller-supplied: they are request-lifecycle
 * metadata (#14/#15), not part of the schema's KuroResult shape.
 *
 * <p>Top-level array order is not a stored fact — children reload ordered by
 * id; the schema treats those arrays as id-keyed sets. The orders that are
 * semantically meaningful (evidenceIds, signalIds, themeIds, claim order)
 * round-trip exactly via the ordinal join columns.
 */
@Service
public class KuroResultPersistence {

    private final SubjectRepository subjects;
    private final KuroResultRepository results;
    private final SourceDocumentRepository documents;
    private final SourceAttributionRepository attributions;
    private final RedactionRepository redactions;
    private final EvidenceRepository evidence;
    private final SignalRepository signals;
    private final SignalEvidenceRepository signalEvidence;
    private final ThemeRepository themes;
    private final ThemeSignalRepository themeSignals;
    private final InferenceClaimRepository inferenceClaims;
    private final InferenceClaimThemeRepository inferenceClaimThemes;
    private final SourceCoverageEntryRepository sourceCoverage;

    KuroResultPersistence(SubjectRepository subjects, KuroResultRepository results,
            SourceDocumentRepository documents, SourceAttributionRepository attributions,
            RedactionRepository redactions, EvidenceRepository evidence, SignalRepository signals,
            SignalEvidenceRepository signalEvidence, ThemeRepository themes, ThemeSignalRepository themeSignals,
            InferenceClaimRepository inferenceClaims, InferenceClaimThemeRepository inferenceClaimThemes,
            SourceCoverageEntryRepository sourceCoverage) {
        this.subjects = subjects;
        this.results = results;
        this.documents = documents;
        this.attributions = attributions;
        this.redactions = redactions;
        this.evidence = evidence;
        this.signals = signals;
        this.signalEvidence = signalEvidence;
        this.themes = themes;
        this.themeSignals = themeSignals;
        this.inferenceClaims = inferenceClaims;
        this.inferenceClaimThemes = inferenceClaimThemes;
        this.sourceCoverage = sourceCoverage;
    }

    @Transactional
    public String save(KuroResult result, String requestId, int version, boolean current) {
        if (results.existsById(result.id())) {
            throw new ResultAlreadyPersistedException(result.id());
        }
        if (!subjects.existsById(result.subject().id())) {
            subjects.save(KuroResultMapper.toEntity(result.subject()));
        }
        results.save(KuroResultMapper.toEntity(result, requestId, version, current));
        switch (result) {
            case KuroResult.Sufficient s -> saveEvidenceChain(s.id(), s.sourceDocuments(), s.sourceAttributions(),
                    s.evidence(), s.signals(), s.themes(), s.inference());
            case KuroResult.Partial p -> saveEvidenceChain(p.id(), p.sourceDocuments(), p.sourceAttributions(),
                    p.evidence(), p.signals(), p.themes(), p.inference());
            case KuroResult.Insufficient i -> {
                saveSources(i.id(), i.sourceDocuments(), i.sourceAttributions());
                if (i.sourceCoverage() != null) {
                    i.sourceCoverage().forEach(c -> sourceCoverage.save(KuroResultMapper.toEntity(c, i.id())));
                }
            }
            case KuroResult.UnsupportedCategory u -> {
                // refusal carries no children beyond the subject and the row itself
            }
        }
        return result.id();
    }

    @Transactional(readOnly = true)
    public Optional<KuroResult> load(String resultId) {
        return results.findById(resultId).map(this::toDomain);
    }

    /**
     * Persists {@code result} as the next version of {@code requestId} and makes
     * it current. The prior current row's is_current flag is flipped to false —
     * the only mutation ever applied to an existing result row (#15). The result
     * content itself is insert-only and immutable.
     *
     * <p>Concurrency: the next version is read-then-written, so two simultaneous
     * re-runs can compute the same version. The DB UNIQUE (request_id, version)
     * and partial-unique ux_kuro_result_current make the losing insert fail
     * cleanly; the api maps that to 409.
     */
    @Transactional
    public String saveNewVersion(KuroResult result, String requestId) {
        int next = results.findMaxVersion(requestId).orElse(0) + 1;
        results.findByRequestIdAndIsCurrentTrue(requestId).ifPresent(prior -> {
            prior.isCurrent = false;
            // Flush the flip before the new row's insert: Hibernate orders inserts
            // ahead of updates, which would momentarily put two is_current=1 rows
            // past ux_kuro_result_current.
            results.saveAndFlush(prior);
        });
        return save(result, requestId, next, true);
    }

    @Transactional(readOnly = true)
    public Optional<KuroResult> loadCurrent(String requestId) {
        return results.findByRequestIdAndIsCurrentTrue(requestId).map(this::toDomain);
    }

    private void saveEvidenceChain(String resultId, List<SourceDocument> docs, List<SourceAttribution> atts,
            List<Evidence> evidenceItems, List<Signal> signalItems, List<Theme> themeItems,
            KuroInference inference) {
        saveSources(resultId, docs, atts);
        // Two passes: SQLite enforces the quality_is_duplicate_of self-FK per
        // statement, and duplicates point at retained originals (never chains).
        var duplicates = evidenceItems.stream()
                .filter(ev -> ev.qualityHints() != null && ev.qualityHints().isDuplicateOf() != null)
                .toList();
        evidenceItems.stream().filter(ev -> !duplicates.contains(ev))
                .forEach(ev -> evidence.save(KuroResultMapper.toEntity(ev, resultId)));
        duplicates.forEach(ev -> evidence.save(KuroResultMapper.toEntity(ev, resultId)));
        for (Signal signal : signalItems) {
            signals.save(KuroResultMapper.toEntity(signal, resultId));
            saveOrdinalJoins(signal.evidenceIds(), (evidenceId, ordinal) -> {
                var join = new SignalEvidenceEntity();
                join.id = KuroResultMapper.newId();
                join.signalId = signal.id();
                join.evidenceId = evidenceId;
                join.ordinal = ordinal;
                signalEvidence.save(join);
            });
        }
        for (Theme theme : themeItems) {
            themes.save(KuroResultMapper.toEntity(theme, resultId));
            saveOrdinalJoins(theme.signalIds(), (signalId, ordinal) -> {
                var join = new ThemeSignalEntity();
                join.id = KuroResultMapper.newId();
                join.themeId = theme.id();
                join.signalId = signalId;
                join.ordinal = ordinal;
                themeSignals.save(join);
            });
        }
        saveClaims(resultId, InferenceClaimEntity.Kind.PATTERNS, inference.patterns());
        saveClaims(resultId, InferenceClaimEntity.Kind.CONSENSUS, inference.consensus());
        saveClaims(resultId, InferenceClaimEntity.Kind.DISAGREEMENTS, inference.disagreements());
        saveClaims(resultId, InferenceClaimEntity.Kind.MAY_SUGGEST, inference.maySuggest());
        saveClaims(resultId, InferenceClaimEntity.Kind.MAY_NOT_SUGGEST, inference.mayNotSuggest());
    }

    private void saveSources(String resultId, List<SourceDocument> docs, List<SourceAttribution> atts) {
        docs.forEach(d -> documents.save(KuroResultMapper.toEntity(d, resultId)));
        for (SourceAttribution att : atts) {
            attributions.save(KuroResultMapper.toEntity(att));
            KuroResultMapper.redactionRows(att).forEach(redactions::save);
        }
    }

    private void saveClaims(String resultId, InferenceClaimEntity.Kind kind,
            List<KuroInference.InferenceClaim> claims) {
        for (int i = 0; i < claims.size(); i++) {
            var claim = claims.get(i);
            var row = new InferenceClaimEntity();
            row.id = KuroResultMapper.newId();
            row.resultId = resultId;
            row.kind = kind;
            row.description = claim.description();
            row.ordinal = i;
            inferenceClaims.save(row);
            saveOrdinalJoins(claim.themeIds(), (themeId, ordinal) -> {
                var join = new InferenceClaimThemeEntity();
                join.id = KuroResultMapper.newId();
                join.inferenceClaimId = row.id;
                join.themeId = themeId;
                join.ordinal = ordinal;
                inferenceClaimThemes.save(join);
            });
        }
    }

    private void saveOrdinalJoins(List<String> ids, OrdinalConsumer consumer) {
        for (int i = 0; i < ids.size(); i++) {
            consumer.accept(ids.get(i), i);
        }
    }

    private interface OrdinalConsumer {
        void accept(String id, int ordinal);
    }

    private KuroResult toDomain(KuroResultEntity row) {
        Subject subject = subjects.findById(row.subjectId).map(KuroResultMapper::toDomain).orElseThrow();
        var docEntities = documents.findByResultIdOrderById(row.id);
        var docs = docEntities.stream().map(KuroResultMapper::toDomain).toList();
        var atts = loadAttributions(docEntities);
        return switch (row.dataSufficiency) {
            case SUFFICIENT -> KuroResultMapper.sufficient(row, subject, docs, atts, loadEvidence(row.id),
                    loadSignals(row.id), loadThemes(row.id), loadInference(row));
            case PARTIAL -> KuroResultMapper.partial(row, subject, docs, atts, loadEvidence(row.id),
                    loadSignals(row.id), loadThemes(row.id), loadInference(row));
            case INSUFFICIENT -> {
                var coverageRows = sourceCoverage.findByResultIdOrderById(row.id);
                var coverage = coverageRows.isEmpty() ? null
                        : coverageRows.stream().map(KuroResultMapper::toDomain).toList();
                yield KuroResultMapper.insufficient(row, subject, docs, atts, coverage);
            }
            case UNSUPPORTED_CATEGORY -> KuroResultMapper.unsupportedCategory(row, subject);
        };
    }

    private List<SourceAttribution> loadAttributions(List<SourceDocumentEntity> docEntities) {
        if (docEntities.isEmpty()) {
            return List.of();
        }
        var attEntities = attributions
                .findBySourceDocumentIdInOrderById(docEntities.stream().map(d -> d.id).toList());
        var redactionsByAttribution = attEntities.isEmpty() ? Map.<String, List<RedactionEntity>>of()
                : redactions.findBySourceAttributionIdInOrderById(attEntities.stream().map(a -> a.id).toList())
                        .stream().collect(groupingBy(r -> r.sourceAttributionId));
        return attEntities.stream()
                .map(a -> KuroResultMapper.toDomain(a, redactionsByAttribution.getOrDefault(a.id, List.of())))
                .toList();
    }

    private List<Evidence> loadEvidence(String resultId) {
        return evidence.findByResultIdOrderById(resultId).stream().map(KuroResultMapper::toDomain).toList();
    }

    private List<Signal> loadSignals(String resultId) {
        var signalEntities = signals.findByResultIdOrderById(resultId);
        var joins = signalEntities.isEmpty() ? List.<SignalEvidenceEntity>of()
                : signalEvidence.findBySignalIdInOrderByOrdinal(signalEntities.stream().map(s -> s.id).toList());
        var evidenceIdsBySignal = idsByParent(joins, j -> j.signalId, j -> j.evidenceId);
        return signalEntities.stream()
                .map(s -> KuroResultMapper.toDomain(s, evidenceIdsBySignal.getOrDefault(s.id, List.of())))
                .toList();
    }

    private List<Theme> loadThemes(String resultId) {
        var themeEntities = themes.findByResultIdOrderById(resultId);
        var joins = themeEntities.isEmpty() ? List.<ThemeSignalEntity>of()
                : themeSignals.findByThemeIdInOrderByOrdinal(themeEntities.stream().map(t -> t.id).toList());
        var signalIdsByTheme = idsByParent(joins, j -> j.themeId, j -> j.signalId);
        return themeEntities.stream()
                .map(t -> KuroResultMapper.toDomain(t, signalIdsByTheme.getOrDefault(t.id, List.of())))
                .toList();
    }

    private KuroInference loadInference(KuroResultEntity row) {
        var claims = inferenceClaims.findByResultIdOrderByOrdinal(row.id);
        var joins = claims.isEmpty() ? List.<InferenceClaimThemeEntity>of()
                : inferenceClaimThemes
                        .findByInferenceClaimIdInOrderByOrdinal(claims.stream().map(c -> c.id).toList());
        var themeIdsByClaim = idsByParent(joins, j -> j.inferenceClaimId, j -> j.themeId);
        return new KuroInference(
                claimsOfKind(claims, InferenceClaimEntity.Kind.PATTERNS, themeIdsByClaim),
                claimsOfKind(claims, InferenceClaimEntity.Kind.CONSENSUS, themeIdsByClaim),
                claimsOfKind(claims, InferenceClaimEntity.Kind.DISAGREEMENTS, themeIdsByClaim),
                row.inferenceCommunitySentimentSummary,
                claimsOfKind(claims, InferenceClaimEntity.Kind.MAY_SUGGEST, themeIdsByClaim),
                claimsOfKind(claims, InferenceClaimEntity.Kind.MAY_NOT_SUGGEST, themeIdsByClaim),
                KuroResultMapper.stringList(row.inferenceLimitationsJson));
    }

    private List<KuroInference.InferenceClaim> claimsOfKind(List<InferenceClaimEntity> claims,
            InferenceClaimEntity.Kind kind, Map<String, List<String>> themeIdsByClaim) {
        return claims.stream().filter(c -> c.kind == kind)
                .map(c -> new KuroInference.InferenceClaim(c.description,
                        themeIdsByClaim.getOrDefault(c.id, List.of())))
                .toList();
    }

    private <J> Map<String, List<String>> idsByParent(List<J> joins,
            java.util.function.Function<J, String> parentId, java.util.function.Function<J, String> childId) {
        return joins.stream()
                .collect(groupingBy(parentId, LinkedHashMap::new, mapping(childId, toList())));
    }
}
