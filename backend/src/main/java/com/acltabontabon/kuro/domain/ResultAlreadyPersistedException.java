package com.acltabontabon.kuro.domain;

/**
 * Thrown when a result row with the given id already exists. Results are
 * insert-only and immutable (#15): a re-run is a new version, never an overwrite
 * of an existing one. Framework-free so it stays in the domain layer.
 */
public class ResultAlreadyPersistedException extends RuntimeException {

    public ResultAlreadyPersistedException(String resultId) {
        super("Result already persisted and is immutable: " + resultId);
    }
}
