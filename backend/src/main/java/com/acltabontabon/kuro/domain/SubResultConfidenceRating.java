package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/primitives.ts
public enum SubResultConfidenceRating implements WireEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String wire;

    SubResultConfidenceRating(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
