package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/result.ts (ResultStatus)
public enum DataSufficiency implements WireEnum {
    SUFFICIENT("sufficient"),
    PARTIAL("partial"),
    INSUFFICIENT("insufficient"),
    UNSUPPORTED_CATEGORY("unsupported_category");

    private final String wire;

    DataSufficiency(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
