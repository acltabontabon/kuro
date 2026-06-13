package com.acltabontabon.kuro.ai.exception;

/**
 * Base of the vendor-neutral AI failure hierarchy (issue #17). Providers
 * translate vendor SDK errors into one of the permitted subtypes so the rest
 * of the backend never imports vendor exceptions. Sealed: the four subtypes
 * below are the complete, closed set of AI failure categories.
 */
public sealed abstract class AiProviderException extends RuntimeException
        permits TransientAiException, TerminalAiException, SchemaViolationAiException, TimeoutAiException {

    protected AiProviderException(String message) {
        super(message);
    }

    protected AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
