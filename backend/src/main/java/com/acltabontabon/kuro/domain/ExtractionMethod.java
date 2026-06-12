package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/evidence.ts
public enum ExtractionMethod implements WireEnum {
    VERBATIM("verbatim"),
    NORMALIZED("normalized"),
    SYNTHESIZED("synthesized");

    private final String wire;

    ExtractionMethod(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
