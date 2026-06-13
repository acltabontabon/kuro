package com.acltabontabon.kuro.ai;

/**
 * Test double for {@link AiProvider} (issue #17): returns a fixed
 * {@link AiCallResponse} and counts calls. Downstream tickets (#19–#24) wire
 * this into their Spring test contexts so they need not wait for a real
 * provider (#18). Plain class with no Spring annotation — register it as a
 * {@code @Bean} where needed.
 */
public final class NoOpAiProvider implements AiProvider {

    private final AiCallResponse response;
    private int callCount;

    public NoOpAiProvider(AiCallResponse response) {
        this.response = response;
    }

    @Override
    public AiCallResponse generateStructured(AiCallRequest request) {
        callCount++;
        return response;
    }

    public int callCount() {
        return callCount;
    }
}
