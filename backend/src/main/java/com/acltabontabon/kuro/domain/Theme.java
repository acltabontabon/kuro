package com.acltabontabon.kuro.domain;

import java.util.List;

// mirrors packages/schemas/src/theme.ts
public record Theme(
        String id,
        String topic,
        Sentiment sentiment,
        List<String> signalIds,
        ThemeConfidence confidence,
        List<ThemeClaim> maySuggest,
        List<ThemeClaim> mayNotSuggest,
        List<String> limitations) {

    public Theme {
        signalIds = List.copyOf(signalIds);
        maySuggest = List.copyOf(maySuggest);
        mayNotSuggest = List.copyOf(mayNotSuggest);
        limitations = List.copyOf(limitations);
    }

    public record ThemeClaim(String description) {
    }
}
