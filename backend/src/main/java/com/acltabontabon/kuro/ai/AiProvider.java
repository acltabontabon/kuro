package com.acltabontabon.kuro.ai;

import com.acltabontabon.kuro.ai.exception.AiProviderException;

/**
 * The vendor-neutral seam between AI model providers and the rest of the
 * backend (issue #17). A provider does exactly one thing: given a fully
 * rendered prompt and a response schema, return a JSON object matching that
 * schema. No vendor SDK type appears in this signature, so extraction and
 * synthesis can swap providers (Gemini, OpenAI, local) without changing.
 *
 * <p>Implementations translate vendor failures into the typed
 * {@link AiProviderException} hierarchy; callers never see vendor exceptions.
 */
public interface AiProvider {

    /**
     * Produce a JSON object matching {@link AiCallRequest#responseSchema()}.
     * The returned {@link AiCallResponse#rawJson()} is unvalidated — schema
     * validation is the caller's responsibility (issue #21).
     *
     * @throws AiProviderException (one of its sealed subtypes) when the call
     *         fails transiently, terminally, on schema violation, or on timeout
     */
    AiCallResponse generateStructured(AiCallRequest request);
}
