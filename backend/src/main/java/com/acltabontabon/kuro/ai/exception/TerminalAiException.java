package com.acltabontabon.kuro.ai.exception;

/**
 * A failure that will not succeed on retry (bad credentials, invalid request,
 * unknown model). Callers should fail fast rather than retry.
 */
public final class TerminalAiException extends AiProviderException {

    public TerminalAiException(String message) {
        super(message);
    }

    public TerminalAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
