package com.acltabontabon.kuro.api;

import com.acltabontabon.kuro.application.RequestNotFoundException;
import com.acltabontabon.kuro.domain.IllegalTransitionException;
import com.acltabontabon.kuro.domain.ResultAlreadyPersistedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps true errors to a consistent {@link ErrorEnvelope}. The product's refusals
 * ({@code unsupported_category}, {@code insufficient}) are results returned with
 * 200 and never reach here.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    ResponseEntity<ErrorEnvelope> badRequest(Exception ex) {
        return envelope(HttpStatus.BAD_REQUEST, "invalid_request", ex.getMessage());
    }

    @ExceptionHandler(RequestNotFoundException.class)
    ResponseEntity<ErrorEnvelope> notFound(RequestNotFoundException ex) {
        return envelope(HttpStatus.NOT_FOUND, "request_not_found", ex.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ResponseEntity<ErrorEnvelope> illegalTransition(IllegalTransitionException ex) {
        return envelope(HttpStatus.CONFLICT, "illegal_transition", ex.getMessage());
    }

    @ExceptionHandler(ResultAlreadyPersistedException.class)
    ResponseEntity<ErrorEnvelope> immutableResult(ResultAlreadyPersistedException ex) {
        return envelope(HttpStatus.CONFLICT, "result_conflict", ex.getMessage());
    }

    /**
     * Only the result version/current uniqueness conflicts map to 409 — a
     * concurrent re-run losing the version race. Any other integrity violation
     * (bad FK, CHECK) is an unexpected bug and propagates to a 500, never masked.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorEnvelope> dataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains("kuro_result")
                && (cause.contains("request_id") || cause.contains("version"))) {
            return envelope(HttpStatus.CONFLICT, "result_conflict",
                    "A newer version of this result was created concurrently");
        }
        throw ex;
    }

    private static ResponseEntity<ErrorEnvelope> envelope(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorEnvelope(code, message));
    }
}
