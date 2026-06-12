package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/subject.ts; description is optional
public record Subject(String id, SubjectKind kind, String displayName, String description) {
}
