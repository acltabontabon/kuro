package com.acltabontabon.kuro.domain;

/**
 * Thrown when a request lifecycle transition is not permitted by
 * {@link RequestLifecycle}. Framework-free so it stays in the domain layer.
 */
public class IllegalTransitionException extends RuntimeException {

    private final RequestStatus from;
    private final RequestStatus to;

    public IllegalTransitionException(RequestStatus from, RequestStatus to) {
        super("Illegal request lifecycle transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public RequestStatus from() {
        return from;
    }

    public RequestStatus to() {
        return to;
    }
}
