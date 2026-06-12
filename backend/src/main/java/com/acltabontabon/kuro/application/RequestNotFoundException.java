package com.acltabontabon.kuro.application;

/** Thrown when a workflow targets a request id that does not exist (api maps to 404). */
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(String requestId) {
        super("Request not found: " + requestId);
    }
}
