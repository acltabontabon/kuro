package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;

/**
 * Backend-native read view of a request for the API (#13): current status plus
 * what it is about. Not part of @kuro/schemas — like {@link KuroRequest}, it is
 * internal to the backend. {@code category}/{@code subjectId}/{@code
 * subjectDisplayName} are null for a refusal request (its requested category is
 * outside DecisionCategory and lives on the result).
 */
public record RequestSummary(
        String id,
        RequestStatus status,
        DecisionCategory category,
        String subjectId,
        String subjectDisplayName,
        OffsetDateTime createdAt) {
}
