package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

// mirrors packages/schemas/src/sourceAttribution.ts; metadata values are
// string/number/boolean/null primitives (not copied: Map.copyOf rejects nulls)
public record SourceAttribution(
        String id,
        String sourceDocumentId,
        SourceType sourceType,
        String url,
        String canonicalUrl,
        String title,
        String authorHandle,
        OffsetDateTime publishedAt,
        OffsetDateTime fetchedAt,
        AccessedVia accessedVia,
        TrustTier trustTier,
        String trustRationale,
        Map<String, Object> metadata,
        List<RedactionRecord> redactions) {

    public SourceAttribution {
        redactions = redactions == null ? null : List.copyOf(redactions);
    }
}
