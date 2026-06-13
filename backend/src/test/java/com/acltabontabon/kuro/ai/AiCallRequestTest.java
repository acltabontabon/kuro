package com.acltabontabon.kuro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.acltabontabon.kuro.domain.AiPhase;
import org.junit.jupiter.api.Test;

class AiCallRequestTest {

    @Test
    void rejectsNullPrompt() {
        assertThatNullPointerException().isThrownBy(() ->
                new AiCallRequest(null, "v1", AiPhase.EXTRACTION, "model-x", "{}", null));
    }

    @Test
    void rejectsNullPromptVersion() {
        assertThatNullPointerException().isThrownBy(() ->
                new AiCallRequest("prompt", null, AiPhase.EXTRACTION, "model-x", "{}", null));
    }

    @Test
    void rejectsNullPhase() {
        assertThatNullPointerException().isThrownBy(() ->
                new AiCallRequest("prompt", "v1", null, "model-x", "{}", null));
    }

    @Test
    void acceptsFullyPopulatedRequest() {
        AiCallOptions options = new AiCallOptions(0.2, 1024, null);
        AiCallRequest request =
                new AiCallRequest("prompt", "v1", AiPhase.SYNTHESIS, "model-x", "{}", options);

        assertThat(request.prompt()).isEqualTo("prompt");
        assertThat(request.phase()).isEqualTo(AiPhase.SYNTHESIS);
        assertThat(request.options()).isEqualTo(options);
    }
}
