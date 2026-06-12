package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.ConfidenceReason;
import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.Evidence;
import com.acltabontabon.kuro.domain.KuroInference;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Locator;
import com.acltabontabon.kuro.domain.RedactionRecord;
import com.acltabontabon.kuro.domain.ResultConfidence;
import com.acltabontabon.kuro.domain.Signal;
import com.acltabontabon.kuro.domain.SignalConfidence;
import com.acltabontabon.kuro.domain.SourceAttribution;
import com.acltabontabon.kuro.domain.SourceDocument;
import com.acltabontabon.kuro.domain.SourceSummary;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.Theme;
import com.acltabontabon.kuro.domain.ThemeConfidence;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;

/**
 * Hand-written mapping between @kuro/schemas-shaped domain types and entity
 * rows. The only place where value objects are flattened to columns,
 * timestamps become ISO-8601 strings, and *_json columns are (de)serialized.
 */
final class KuroResultMapper {

    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };
    private static final TypeReference<List<ConfidenceReason>> REASONS = new TypeReference<>() {
    };
    private static final TypeReference<List<Theme.ThemeClaim>> THEME_CLAIMS = new TypeReference<>() {
    };
    private static final TypeReference<List<KuroResult.EvidenceGap>> EVIDENCE_GAPS = new TypeReference<>() {
    };
    private static final TypeReference<List<KuroResult.SuggestedNextSource>> NEXT_SOURCES = new TypeReference<>() {
    };
    private static final TypeReference<List<DecisionCategory>> CATEGORIES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> METADATA = new TypeReference<>() {
    };

    private KuroResultMapper() {
    }

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static String iso(OffsetDateTime t) {
        return t == null ? null : t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    static OffsetDateTime ts(String s) {
        return s == null ? null : OffsetDateTime.parse(s);
    }

    static List<String> stringList(String json) {
        return KuroJson.read(json, STRINGS);
    }

    // ---- subject

    static SubjectEntity toEntity(Subject s) {
        var e = new SubjectEntity();
        e.id = s.id();
        e.kind = s.kind();
        e.displayName = s.displayName();
        e.description = s.description();
        return e;
    }

    static Subject toDomain(SubjectEntity e) {
        return new Subject(e.id, e.kind, e.displayName, e.description);
    }

    // ---- kuro_result row

    static KuroResultEntity toEntity(KuroResult r, String requestId, int version, boolean current) {
        var e = new KuroResultEntity();
        e.id = r.id();
        e.requestId = requestId;
        e.version = version;
        e.isCurrent = current;
        e.subjectId = r.subject().id();
        e.dataSufficiency = r.dataSufficiency();
        e.generatedAt = iso(r.generatedAt());
        e.limitationsJson = KuroJson.write(r.limitations());
        switch (r) {
            case KuroResult.Sufficient s -> {
                e.category = s.category();
                e.summary = s.summary();
                e.finalKuro = s.finalKuro();
                setConfidence(e, s.confidence());
                setInference(e, s.inference());
                e.sourceSummaryJson = KuroJson.write(s.sourceSummary());
            }
            case KuroResult.Partial p -> {
                e.category = p.category();
                e.summary = p.summary();
                e.finalKuro = p.finalKuro();
                setConfidence(e, p.confidence());
                setInference(e, p.inference());
                e.sourceSummaryJson = KuroJson.write(p.sourceSummary());
                e.evidenceGapsJson = KuroJson.write(p.evidenceGaps());
            }
            case KuroResult.Insufficient i -> {
                e.category = i.category();
                e.summary = i.summary();
                e.finalKuro = i.finalKuro();
                setConfidence(e, i.confidence());
                e.sourceSummaryJson = KuroJson.write(i.sourceSummary());
                e.insufficientReasonKind = i.insufficientDataReason().kind();
                e.insufficientReasonExplanation = i.insufficientDataReason().explanation();
                e.suggestedNextSourcesJson = KuroJson.write(i.suggestedNextSources());
            }
            case KuroResult.UnsupportedCategory u -> {
                e.requestedCategory = u.requestedCategory();
                e.supportedCategoriesJson = KuroJson.write(u.supportedCategories());
                e.refusalMessage = u.refusalMessage();
            }
        }
        return e;
    }

    private static void setConfidence(KuroResultEntity e, ResultConfidence c) {
        e.confidenceRating = c.rating();
        e.confidenceSupportScore = c.supportScore();
        var in = c.inputs();
        e.confidenceInputSourceCount = in.sourceCount();
        e.confidenceInputSourceDiversity = in.sourceDiversity();
        e.confidenceInputSourceFreshness = in.sourceFreshness();
        e.confidenceInputSignalConsistency = in.signalConsistency();
        e.confidenceInputThemeSupportAggregate = in.themeSupportAggregate();
        e.confidenceInputTopicBreadth = in.topicBreadth();
        e.confidenceReasonsJson = KuroJson.write(c.reasons());
    }

    private static ResultConfidence confidence(KuroResultEntity e) {
        return new ResultConfidence(
                e.confidenceRating,
                e.confidenceSupportScore,
                new ResultConfidence.Inputs(
                        e.confidenceInputSourceCount,
                        e.confidenceInputSourceDiversity,
                        e.confidenceInputSourceFreshness,
                        e.confidenceInputSignalConsistency,
                        e.confidenceInputThemeSupportAggregate,
                        e.confidenceInputTopicBreadth),
                KuroJson.read(e.confidenceReasonsJson, REASONS));
    }

    private static void setInference(KuroResultEntity e, KuroInference inference) {
        e.inferenceCommunitySentimentSummary = inference.communitySentimentSummary();
        e.inferenceLimitationsJson = KuroJson.write(inference.limitations());
    }

    // ---- variant assembly from a loaded row

    static KuroResult.Sufficient sufficient(KuroResultEntity e, Subject subject, List<SourceDocument> documents,
            List<SourceAttribution> attributions, List<Evidence> evidence, List<Signal> signals,
            List<Theme> themes, KuroInference inference) {
        return new KuroResult.Sufficient(e.id, subject, ts(e.generatedAt), KuroJson.read(e.limitationsJson, STRINGS),
                e.category, e.summary, documents, attributions, evidence, signals, themes, inference,
                KuroJson.read(e.sourceSummaryJson, SourceSummary.class), confidence(e), e.finalKuro);
    }

    static KuroResult.Partial partial(KuroResultEntity e, Subject subject, List<SourceDocument> documents,
            List<SourceAttribution> attributions, List<Evidence> evidence, List<Signal> signals,
            List<Theme> themes, KuroInference inference) {
        return new KuroResult.Partial(e.id, subject, ts(e.generatedAt), KuroJson.read(e.limitationsJson, STRINGS),
                e.category, e.summary, documents, attributions, evidence, signals, themes, inference,
                KuroJson.read(e.sourceSummaryJson, SourceSummary.class), confidence(e), e.finalKuro,
                KuroJson.read(e.evidenceGapsJson, EVIDENCE_GAPS));
    }

    static KuroResult.Insufficient insufficient(KuroResultEntity e, Subject subject, List<SourceDocument> documents,
            List<SourceAttribution> attributions, List<KuroResult.SourceCoverageEntry> sourceCoverage) {
        return new KuroResult.Insufficient(e.id, subject, ts(e.generatedAt), KuroJson.read(e.limitationsJson, STRINGS),
                e.category, e.summary, documents, attributions,
                KuroJson.read(e.sourceSummaryJson, SourceSummary.class), sourceCoverage,
                new KuroResult.InsufficientDataReason(e.insufficientReasonKind, e.insufficientReasonExplanation),
                KuroJson.read(e.suggestedNextSourcesJson, NEXT_SOURCES), confidence(e), e.finalKuro);
    }

    static KuroResult.UnsupportedCategory unsupportedCategory(KuroResultEntity e, Subject subject) {
        return new KuroResult.UnsupportedCategory(e.id, subject, ts(e.generatedAt),
                KuroJson.read(e.limitationsJson, STRINGS), e.requestedCategory,
                KuroJson.read(e.supportedCategoriesJson, CATEGORIES), e.refusalMessage);
    }

    // ---- source documents and attribution

    static SourceDocumentEntity toEntity(SourceDocument d, String resultId) {
        var e = new SourceDocumentEntity();
        e.id = d.id();
        e.resultId = resultId;
        e.url = d.url();
        e.platform = d.platform();
        e.author = d.author();
        e.capturedAt = iso(d.capturedAt());
        e.publishedAt = iso(d.publishedAt());
        e.content = d.content();
        e.contentHash = d.contentHash();
        e.context = d.context();
        return e;
    }

    static SourceDocument toDomain(SourceDocumentEntity e) {
        return new SourceDocument(e.id, e.url, e.platform, e.author, ts(e.capturedAt), ts(e.publishedAt),
                e.content, e.contentHash, e.context);
    }

    static SourceAttributionEntity toEntity(SourceAttribution a) {
        var e = new SourceAttributionEntity();
        e.id = a.id();
        e.sourceDocumentId = a.sourceDocumentId();
        e.sourceType = a.sourceType();
        e.url = a.url();
        e.canonicalUrl = a.canonicalUrl();
        e.title = a.title();
        e.authorHandle = a.authorHandle();
        e.publishedAt = iso(a.publishedAt());
        e.fetchedAt = iso(a.fetchedAt());
        e.accessedVia = a.accessedVia();
        e.trustTier = a.trustTier();
        e.trustRationale = a.trustRationale();
        e.metadataJson = KuroJson.write(a.metadata());
        return e;
    }

    static List<RedactionEntity> redactionRows(SourceAttribution a) {
        if (a.redactions() == null) {
            return List.of();
        }
        return a.redactions().stream().map(r -> {
            var e = new RedactionEntity();
            e.id = newId();
            e.sourceAttributionId = a.id();
            e.field = r.field();
            e.category = r.category();
            e.reason = r.reason();
            return e;
        }).toList();
    }

    static SourceAttribution toDomain(SourceAttributionEntity e, List<RedactionEntity> redactionRows) {
        List<RedactionRecord> redactions = redactionRows.isEmpty() ? null
                : redactionRows.stream().map(r -> new RedactionRecord(r.field, r.category, r.reason)).toList();
        return new SourceAttribution(e.id, e.sourceDocumentId, e.sourceType, e.url, e.canonicalUrl, e.title,
                e.authorHandle, ts(e.publishedAt), ts(e.fetchedAt), e.accessedVia, e.trustTier, e.trustRationale,
                KuroJson.read(e.metadataJson, METADATA), redactions);
    }

    // ---- evidence

    static EvidenceEntity toEntity(Evidence ev, String resultId) {
        var e = new EvidenceEntity();
        e.id = ev.id();
        e.resultId = resultId;
        e.sourceDocumentId = ev.sourceDocumentId();
        e.snippet = ev.snippet();
        e.originalSnippet = ev.originalSnippet();
        e.locatorKind = ev.locator().kind();
        switch (ev.locator()) {
            case Locator.CharRange c -> {
                e.locatorStart = c.start();
                e.locatorEnd = c.end();
            }
            case Locator.LineRange l -> {
                e.locatorStartLine = l.startLine();
                e.locatorEndLine = l.endLine();
            }
            case Locator.Anchor a -> e.locatorAnchor = a.value();
        }
        e.extractionMethod = ev.extraction().method();
        e.extractedAt = iso(ev.extraction().extractedAt());
        e.extractor = ev.extraction().extractor();
        var quality = ev.qualityHints();
        if (quality != null) {
            e.qualitySourceTrust = quality.sourceTrust();
            e.qualityIsDuplicateOf = quality.isDuplicateOf();
            e.qualityNotes = quality.notes();
        }
        return e;
    }

    static Evidence toDomain(EvidenceEntity e) {
        Locator locator = switch (e.locatorKind) {
            case CHAR_RANGE -> new Locator.CharRange(e.locatorStart, e.locatorEnd);
            case LINE_RANGE -> new Locator.LineRange(e.locatorStartLine, e.locatorEndLine);
            case ANCHOR -> new Locator.Anchor(e.locatorAnchor);
        };
        var quality = e.qualitySourceTrust == null && e.qualityIsDuplicateOf == null && e.qualityNotes == null
                ? null
                : new Evidence.QualityHints(e.qualitySourceTrust, e.qualityIsDuplicateOf, e.qualityNotes);
        return new Evidence(e.id, e.sourceDocumentId, e.snippet, e.originalSnippet, locator,
                new Evidence.Extraction(e.extractionMethod, ts(e.extractedAt), e.extractor), quality);
    }

    // ---- signals

    static SignalEntity toEntity(Signal s, String resultId) {
        var e = new SignalEntity();
        e.id = s.id();
        e.resultId = resultId;
        e.topic = s.topic();
        e.sentiment = s.sentiment();
        e.claim = s.claim();
        var c = s.confidence();
        e.confidenceRating = c.rating();
        e.confidenceSupportScore = c.supportScore();
        var in = c.inputs();
        e.confidenceInputSourceCount = in.sourceCount();
        e.confidenceInputSourceDiversity = in.sourceDiversity();
        e.confidenceInputSourceFreshness = in.sourceFreshness();
        e.confidenceInputSignalConsistency = in.signalConsistency();
        e.confidenceInputClarity = in.clarity();
        e.confidenceInputLanguageAmbiguity = in.languageAmbiguity();
        e.confidenceInputDirectnessOfSupport = in.directnessOfSupport();
        e.confidenceReasonsJson = KuroJson.write(c.reasons());
        return e;
    }

    static Signal toDomain(SignalEntity e, List<String> evidenceIds) {
        var confidence = new SignalConfidence(
                e.confidenceRating,
                e.confidenceSupportScore,
                new SignalConfidence.Inputs(
                        e.confidenceInputSourceCount,
                        e.confidenceInputSourceDiversity,
                        e.confidenceInputSourceFreshness,
                        e.confidenceInputSignalConsistency,
                        e.confidenceInputClarity,
                        e.confidenceInputLanguageAmbiguity,
                        e.confidenceInputDirectnessOfSupport),
                KuroJson.read(e.confidenceReasonsJson, REASONS));
        return new Signal(e.id, e.topic, e.sentiment, e.claim, evidenceIds, confidence);
    }

    // ---- themes

    static ThemeEntity toEntity(Theme t, String resultId) {
        var e = new ThemeEntity();
        e.id = t.id();
        e.resultId = resultId;
        e.topic = t.topic();
        e.sentiment = t.sentiment();
        var c = t.confidence();
        e.confidenceRating = c.rating();
        e.confidenceSupportScore = c.supportScore();
        var in = c.inputs();
        e.confidenceInputSourceCount = in.sourceCount();
        e.confidenceInputSourceDiversity = in.sourceDiversity();
        e.confidenceInputSourceFreshness = in.sourceFreshness();
        e.confidenceInputSignalConsistency = in.signalConsistency();
        e.confidenceReasonsJson = KuroJson.write(c.reasons());
        e.maySuggestJson = KuroJson.write(t.maySuggest());
        e.mayNotSuggestJson = KuroJson.write(t.mayNotSuggest());
        e.limitationsJson = KuroJson.write(t.limitations());
        return e;
    }

    static Theme toDomain(ThemeEntity e, List<String> signalIds) {
        var confidence = new ThemeConfidence(
                e.confidenceRating,
                e.confidenceSupportScore,
                new ThemeConfidence.Inputs(
                        e.confidenceInputSourceCount,
                        e.confidenceInputSourceDiversity,
                        e.confidenceInputSourceFreshness,
                        e.confidenceInputSignalConsistency),
                KuroJson.read(e.confidenceReasonsJson, REASONS));
        return new Theme(e.id, e.topic, e.sentiment, signalIds, confidence,
                KuroJson.read(e.maySuggestJson, THEME_CLAIMS), KuroJson.read(e.mayNotSuggestJson, THEME_CLAIMS),
                KuroJson.read(e.limitationsJson, STRINGS));
    }

    // ---- source coverage

    static SourceCoverageEntryEntity toEntity(KuroResult.SourceCoverageEntry entry, String resultId) {
        var e = new SourceCoverageEntryEntity();
        e.id = newId();
        e.resultId = resultId;
        e.sourceDocumentId = entry.sourceDocumentId();
        e.assessment = entry.assessment();
        e.note = entry.note();
        return e;
    }

    static KuroResult.SourceCoverageEntry toDomain(SourceCoverageEntryEntity e) {
        return new KuroResult.SourceCoverageEntry(e.sourceDocumentId, e.assessment, e.note);
    }
}
