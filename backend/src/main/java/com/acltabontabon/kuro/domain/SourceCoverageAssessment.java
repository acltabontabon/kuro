package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/result.ts
public enum SourceCoverageAssessment implements WireEnum {
    SPAM("spam"),
    DUPLICATE("duplicate"),
    INACCESSIBLE("inaccessible"),
    UNRELATED("unrelated"),
    TOO_VAGUE("too_vague"),
    NOT_ABOUT_SUBJECT("not_about_subject"),
    STALE("stale"),
    PROMOTIONAL("promotional"),
    OTHER("other");

    private final String wire;

    SourceCoverageAssessment(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
