package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;

// mirrors packages/schemas/src/evidence.ts; originalSnippet/qualityHints optional
public record Evidence(
        String id,
        String sourceDocumentId,
        String snippet,
        String originalSnippet,
        Locator locator,
        Extraction extraction,
        QualityHints qualityHints) {

    public record Extraction(ExtractionMethod method, OffsetDateTime extractedAt, String extractor) {
    }

    public record QualityHints(SourceTrust sourceTrust, String isDuplicateOf, String notes) {
    }
}
