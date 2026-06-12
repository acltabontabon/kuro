package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;

// mirrors packages/schemas/src/sourceDocument.ts; author/publishedAt are nullable
// by design (PII posture), content/contentHash/context are optional
public record SourceDocument(
        String id,
        String url,
        String platform,
        String author,
        OffsetDateTime capturedAt,
        OffsetDateTime publishedAt,
        String content,
        String contentHash,
        String context) {
}
