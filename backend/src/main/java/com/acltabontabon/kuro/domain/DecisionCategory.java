package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/decisionCategory.ts
public enum DecisionCategory implements WireEnum {
    EMPLOYMENT_INTELLIGENCE("employment_intelligence", SubjectKind.EMPLOYER),
    RENTAL_INTELLIGENCE("rental_intelligence", SubjectKind.RENTAL);

    private final String wire;
    private final SubjectKind subjectKind;

    DecisionCategory(String wire, SubjectKind subjectKind) {
        this.wire = wire;
        this.subjectKind = subjectKind;
    }

    @Override
    public String wire() {
        return wire;
    }

    /** The subject kind this category produces results about. */
    public SubjectKind subjectKind() {
        return subjectKind;
    }
}
