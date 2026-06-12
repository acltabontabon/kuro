package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/result.ts
public enum InsufficientDataReasonKind implements WireEnum {
    NO_SOURCES_FOUND("no_sources_found"),
    NO_USABLE_EVIDENCE("no_usable_evidence"),
    SUBJECT_UNIDENTIFIABLE("subject_unidentifiable"),
    OUT_OF_WINDOW("out_of_window"),
    OTHER("other");

    private final String wire;

    InsufficientDataReasonKind(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
