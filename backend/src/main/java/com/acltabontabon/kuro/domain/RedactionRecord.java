package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/sourceAttribution.ts; reason is optional
public record RedactionRecord(String field, RedactionCategory category, String reason) {
}
