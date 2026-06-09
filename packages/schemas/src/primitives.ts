import { z } from "zod";

export const Id = z.string().min(1);
export type Id = z.infer<typeof Id>;

export const Url = z.string().url();
export type Url = z.infer<typeof Url>;

export const IsoDateTime = z.string().datetime({ offset: true });
export type IsoDateTime = z.infer<typeof IsoDateTime>;

export const SupportScore = z.number().min(0).max(1);
export type SupportScore = z.infer<typeof SupportScore>;

export const SubResultConfidenceRating = z.enum(["low", "medium", "high"]);
export type SubResultConfidenceRating = z.infer<typeof SubResultConfidenceRating>;

export const ResultConfidenceRating = z.enum(["low", "medium", "high", "unknown"]);
export type ResultConfidenceRating = z.infer<typeof ResultConfidenceRating>;

export const ConfidenceDriver = z.enum([
  "sourceCount",
  "sourceDiversity",
  "sourceFreshness",
  "signalConsistency",
  "clarity",
  "languageAmbiguity",
  "directnessOfSupport",
  "themeSupportAggregate",
  "topicBreadth",
]);
export type ConfidenceDriver = z.infer<typeof ConfidenceDriver>;

export const ConfidenceEffect = z.enum(["raises", "lowers", "neutral"]);
export type ConfidenceEffect = z.infer<typeof ConfidenceEffect>;
