package com.acltabontabon.kuro.api;

/**
 * Consistent error body for true errors (validation, not-found, not-ready). The
 * product's refusals ({@code unsupported_category}, {@code insufficient}) are
 * results, not errors, and never use this envelope.
 */
record ErrorEnvelope(String code, String message) {
}
