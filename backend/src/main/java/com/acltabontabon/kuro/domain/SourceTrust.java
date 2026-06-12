package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/evidence.ts — evidence-level hint, distinct from TrustTier
public enum SourceTrust implements WireEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String wire;

    SourceTrust(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
