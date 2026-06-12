package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/signal.ts
public enum Sentiment implements WireEnum {
    POSITIVE("positive"),
    NEGATIVE("negative"),
    NEUTRAL("neutral"),
    MIXED("mixed");

    private final String wire;

    Sentiment(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
