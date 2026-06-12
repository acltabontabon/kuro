package com.acltabontabon.kuro.api;

import jakarta.validation.constraints.NotBlank;

/** Subject as supplied on the wire; {@code id} is optional (see resolveSubject). */
record SubjectDto(
        String id,
        @NotBlank String kind,
        @NotBlank String displayName,
        String description) {
}
