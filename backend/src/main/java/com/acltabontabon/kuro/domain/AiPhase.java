package com.acltabontabon.kuro.domain;

// Backend-native AI pipeline phases (issue #16); stored verbatim in ai_run.phase.
public enum AiPhase implements WireEnum {
    EXTRACTION("extraction"),
    SYNTHESIS("synthesis");

    private final String wire;

    AiPhase(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
