package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/primitives.ts
public enum ResultConfidenceRating implements WireEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    UNKNOWN("unknown");

    private final String wire;

    ResultConfidenceRating(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
