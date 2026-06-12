package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/subject.ts
public enum SubjectKind implements WireEnum {
    EMPLOYER("employer"),
    RENTAL("rental"),
    PRODUCT("product"),
    SERVICE("service"),
    LOCATION("location"),
    ROLE("role"),
    OTHER("other");

    private final String wire;

    SubjectKind(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
