package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/evidence.ts — wire values keep the schema's camelCase
public enum LocatorKind implements WireEnum {
    CHAR_RANGE("charRange"),
    LINE_RANGE("lineRange"),
    ANCHOR("anchor");

    private final String wire;

    LocatorKind(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
