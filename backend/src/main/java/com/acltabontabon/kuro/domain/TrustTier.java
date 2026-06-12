package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/sourceAttribution.ts
public enum TrustTier implements WireEnum {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    COMMUNITY("community"),
    LOW_CONTEXT("low_context"),
    UNKNOWN("unknown");

    private final String wire;

    TrustTier(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
