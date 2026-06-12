package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/primitives.ts
public enum ConfidenceEffect implements WireEnum {
    RAISES("raises"),
    LOWERS("lowers"),
    NEUTRAL("neutral");

    private final String wire;

    ConfidenceEffect(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
