package com.acltabontabon.kuro.ai.exception;

/**
 * A failure that may succeed on retry (rate limit, overload, network blip).
 * Retry policy lives in #22; this type only marks the call as retryable.
 */
public final class TransientAiException extends AiProviderException {

    public TransientAiException(String message) {
        super(message);
    }

    public TransientAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
