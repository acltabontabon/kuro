import { z } from "zod";
import { IsoDateTime } from "./primitives.js";
import { SourceType, TrustTier } from "./sourceAttribution.js";

export const PlatformCount = z.object({
  platform: z.string().min(1),
  count: z.number().int().min(0),
});
export type PlatformCount = z.infer<typeof PlatformCount>;

export const SourceTypeCount = z.object({
  sourceType: SourceType,
  count: z.number().int().min(0),
});
export type SourceTypeCount = z.infer<typeof SourceTypeCount>;

export const TrustTierCount = z.object({
  trustTier: TrustTier,
  count: z.number().int().min(0),
});
export type TrustTierCount = z.infer<typeof TrustTierCount>;

export const SourceExclusion = z.object({
  reason: z.string().min(1),
  count: z.number().int().min(0),
});
export type SourceExclusion = z.infer<typeof SourceExclusion>;

export const SourceFreshness = z.object({
  oldestPublishedAt: IsoDateTime,
  newestPublishedAt: IsoDateTime,
}).refine((f) => f.oldestPublishedAt <= f.newestPublishedAt, {
  message: "Source freshness oldestPublishedAt must be <= newestPublishedAt",
});
export type SourceFreshness = z.infer<typeof SourceFreshness>;

export const SourceSummary = z.object({
  documentCount: z.number().int().min(0),
  platforms: z.array(PlatformCount),
  sourceTypes: z.array(SourceTypeCount),
  trustTiers: z.array(TrustTierCount),
  exclusions: z.array(SourceExclusion),
  freshness: SourceFreshness.nullable(),
  diversityNotes: z.array(z.string().min(1)),
});
export type SourceSummary = z.infer<typeof SourceSummary>;
