package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/primitives.ts — wire values keep the schema's camelCase
public enum ConfidenceDriver implements WireEnum {
    SOURCE_COUNT("sourceCount"),
    SOURCE_DIVERSITY("sourceDiversity"),
    SOURCE_FRESHNESS("sourceFreshness"),
    SIGNAL_CONSISTENCY("signalConsistency"),
    CLARITY("clarity"),
    LANGUAGE_AMBIGUITY("languageAmbiguity"),
    DIRECTNESS_OF_SUPPORT("directnessOfSupport"),
    THEME_SUPPORT_AGGREGATE("themeSupportAggregate"),
    TOPIC_BREADTH("topicBreadth");

    private final String wire;

    ConfidenceDriver(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
