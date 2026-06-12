package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/sourceAttribution.ts
public enum RedactionCategory implements WireEnum {
    PII("pii"),
    PRIVATE_ID("private_id"),
    EMAIL("email"),
    REAL_NAME("real_name"),
    HIDDEN_METADATA("hidden_metadata"),
    OTHER("other");

    private final String wire;

    RedactionCategory(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
