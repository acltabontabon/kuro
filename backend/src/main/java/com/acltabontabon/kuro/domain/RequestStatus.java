package com.acltabontabon.kuro.domain;

// Backend-native lifecycle states (issue #14); stored verbatim in kuro_request.status
public enum RequestStatus implements WireEnum {
    CREATED,
    COLLECTING,
    EXTRACTING,
    SYNTHESIZING,
    READY,
    FAILED;

    @Override
    public String wire() {
        return name();
    }
}
