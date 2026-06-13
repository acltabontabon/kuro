package com.acltabontabon.kuro.ai.exception;

/**
 * The call exceeded its time budget ({@code AiCallOptions.timeout} or the
 * provider default) before a response arrived. Timeout policy lives in #22.
 */
public final class TimeoutAiException extends AiProviderException {

    public TimeoutAiException(String message) {
        super(message);
    }

    public TimeoutAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
