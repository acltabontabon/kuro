package com.acltabontabon.kuro.ai.exception;

/**
 * The provider returned output that does not match the requested response
 * schema, and the provider itself detected this (e.g. structured-output mode
 * rejected the generation). Caller-side validation (#21) is separate.
 */
public final class SchemaViolationAiException extends AiProviderException {

    public SchemaViolationAiException(String message) {
        super(message);
    }

    public SchemaViolationAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
