package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;

// Backend-native request lifecycle (#14); one row per user request
public record KuroRequest(String id, RequestStatus status, OffsetDateTime createdAt) {
}
