package com.acltabontabon.kuro.domain;

import java.util.List;

// mirrors packages/schemas/src/inference.ts
public record KuroInference(
        List<InferenceClaim> patterns,
        List<InferenceClaim> consensus,
        List<InferenceClaim> disagreements,
        String communitySentimentSummary,
        List<InferenceClaim> maySuggest,
        List<InferenceClaim> mayNotSuggest,
        List<String> limitations) {

    public KuroInference {
        patterns = List.copyOf(patterns);
        consensus = List.copyOf(consensus);
        disagreements = List.copyOf(disagreements);
        maySuggest = List.copyOf(maySuggest);
        mayNotSuggest = List.copyOf(mayNotSuggest);
        limitations = List.copyOf(limitations);
    }

    public record InferenceClaim(String description, List<String> themeIds) {
        public InferenceClaim {
            themeIds = List.copyOf(themeIds);
        }
    }
}
