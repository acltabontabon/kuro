import { z } from "zod";
import {
  ConfidenceDriver,
  ConfidenceEffect,
  ResultConfidenceRating,
  SubResultConfidenceRating,
  SupportScore,
} from "./primitives.js";

export const ConfidenceReason = z
  .object({
    driver: ConfidenceDriver,
    effect: ConfidenceEffect,
    note: z.string().min(1),
  })
  .strict();
export type ConfidenceReason = z.infer<typeof ConfidenceReason>;

const BaseInputs = z.object({
  sourceCount: z.number().int().min(0).optional(),
  sourceDiversity: SupportScore.optional(),
  sourceFreshness: SupportScore.optional(),
  signalConsistency: SupportScore.optional(),
});
export type BaseConfidenceInputs = z.infer<typeof BaseInputs>;

export const SignalConfidence = z.object({
  level: z.literal("signal"),
  rating: SubResultConfidenceRating,
  /** @internal — not part of the public API response shape */
  supportScore: SupportScore.optional(),
  /** @internal — not part of the public API response shape */
  inputs: BaseInputs.extend({
    clarity: SupportScore.optional(),
    languageAmbiguity: SupportScore.optional(),
    directnessOfSupport: SupportScore.optional(),
  }),
  reasons: z.array(ConfidenceReason).min(1),
});
export type SignalConfidence = z.infer<typeof SignalConfidence>;

export const ThemeConfidence = z.object({
  level: z.literal("theme"),
  rating: SubResultConfidenceRating,
  /** @internal — not part of the public API response shape */
  supportScore: SupportScore.optional(),
  /** @internal — not part of the public API response shape */
  inputs: BaseInputs,
  reasons: z.array(ConfidenceReason).min(1),
});
export type ThemeConfidence = z.infer<typeof ThemeConfidence>;

export const ResultConfidence = z.object({
  level: z.literal("result"),
  rating: ResultConfidenceRating,
  /** @internal — not part of the public API response shape */
  supportScore: SupportScore.optional(),
  /** @internal — not part of the public API response shape */
  inputs: BaseInputs.extend({
    themeSupportAggregate: SupportScore.optional(),
    topicBreadth: SupportScore.optional(),
  }),
  reasons: z.array(ConfidenceReason).min(1),
});
export type ResultConfidence = z.infer<typeof ResultConfidence>;

export const Confidence = z.discriminatedUnion("level", [
  SignalConfidence,
  ThemeConfidence,
  ResultConfidence,
]);
export type Confidence = z.infer<typeof Confidence>;
