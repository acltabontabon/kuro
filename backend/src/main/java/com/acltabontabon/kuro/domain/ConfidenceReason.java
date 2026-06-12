package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/confidence.ts
public record ConfidenceReason(ConfidenceDriver driver, ConfidenceEffect effect, String note) {
}
