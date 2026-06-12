package com.acltabontabon.kuro.domain;

import java.time.OffsetDateTime;
import java.util.List;

// mirrors packages/schemas/src/sourceSummary.ts; freshness is nullable
public record SourceSummary(
        int documentCount,
        List<PlatformCount> platforms,
        List<SourceTypeCount> sourceTypes,
        List<TrustTierCount> trustTiers,
        List<SourceExclusion> exclusions,
        Freshness freshness,
        List<String> diversityNotes) {

    public SourceSummary {
        platforms = List.copyOf(platforms);
        sourceTypes = List.copyOf(sourceTypes);
        trustTiers = List.copyOf(trustTiers);
        exclusions = List.copyOf(exclusions);
        diversityNotes = List.copyOf(diversityNotes);
    }

    public record PlatformCount(String platform, int count) {
    }

    public record SourceTypeCount(SourceType sourceType, int count) {
    }

    public record TrustTierCount(TrustTier trustTier, int count) {
    }

    public record SourceExclusion(String reason, int count) {
    }

    public record Freshness(OffsetDateTime oldestPublishedAt, OffsetDateTime newestPublishedAt) {
    }
}
