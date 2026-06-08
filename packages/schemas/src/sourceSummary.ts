import { z } from "zod";
import { IsoDateTime } from "./primitives.js";

export const PlatformCount = z.object({
  platform: z.string().min(1),
  count: z.number().int().min(0),
});
export type PlatformCount = z.infer<typeof PlatformCount>;

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
  exclusions: z.array(SourceExclusion),
  freshness: SourceFreshness.nullable(),
  diversityNotes: z.array(z.string().min(1)),
});
export type SourceSummary = z.infer<typeof SourceSummary>;
