package com.acltabontabon.kuro.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.acltabontabon.kuro.domain.AiPhase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NoOpAiProviderTest {

    private static final AiCallResponse CANNED =
            new AiCallResponse("{\"ok\":true}", 10, 20, Duration.ofMillis(5), "call-1");

    @Test
    void returnsConfiguredResponseAndCountsCalls() {
        NoOpAiProvider provider = new NoOpAiProvider(CANNED);
        AiCallRequest request =
                new AiCallRequest("prompt", "v1", AiPhase.EXTRACTION, "model-x", "{}", null);

        assertThat(provider.callCount()).isZero();
        assertThat(provider.generateStructured(request)).isEqualTo(CANNED);
        assertThat(provider.generateStructured(request)).isEqualTo(CANNED);
        assertThat(provider.callCount()).isEqualTo(2);
    }
}
