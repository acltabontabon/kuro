package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/sourceAttribution.ts
public enum AccessedVia implements WireEnum {
    DIRECT_FETCH("direct_fetch"),
    USER_PASTE("user_paste"),
    FILE_UPLOAD("file_upload"),
    API_IMPORT("api_import"),
    OTHER("other");

    private final String wire;

    AccessedVia(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
