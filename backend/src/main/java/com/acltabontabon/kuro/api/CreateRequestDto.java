package com.acltabontabon.kuro.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create-request body. {@code category} is a raw string (not the DecisionCategory
 * enum) so an out-of-scope value parses and yields the structured refusal rather
 * than a deserialization error.
 */
record CreateRequestDto(
        @NotBlank String category,
        @NotNull @Valid SubjectDto subject) {
}
