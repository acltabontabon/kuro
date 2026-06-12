package com.acltabontabon.kuro.domain;

import java.util.List;

// mirrors packages/schemas/src/signal.ts
public record Signal(
        String id,
        String topic,
        Sentiment sentiment,
        String claim,
        List<String> evidenceIds,
        SignalConfidence confidence) {

    public Signal {
        evidenceIds = List.copyOf(evidenceIds);
    }
}
