import { z } from "zod";
import { ConfidenceRating, SupportScore } from "./primitives.js";

const BaseInputs = z.object({
  sourceCount: z.number().int().min(0).optional(),
  sourceDiversity: SupportScore.optional(),
  sourceFreshness: SupportScore.optional(),
  signalConsistency: SupportScore.optional(),
});
export type BaseConfidenceInputs = z.infer<typeof BaseInputs>;

export const SignalConfidence = z.object({
  level: z.literal("signal"),
  rating: ConfidenceRating,
  supportScore: SupportScore.optional(),
  inputs: BaseInputs.extend({
    clarity: SupportScore.optional(),
    languageAmbiguity: SupportScore.optional(),
    directnessOfSupport: SupportScore.optional(),
  }),
});
export type SignalConfidence = z.infer<typeof SignalConfidence>;

export const ThemeConfidence = z.object({
  level: z.literal("theme"),
  rating: ConfidenceRating,
  supportScore: SupportScore.optional(),
  inputs: BaseInputs,
});
export type ThemeConfidence = z.infer<typeof ThemeConfidence>;

export const ResultConfidence = z.object({
  level: z.literal("result"),
  rating: ConfidenceRating,
  supportScore: SupportScore.optional(),
  inputs: BaseInputs.extend({
    themeSupportAggregate: SupportScore.optional(),
    topicBreadth: SupportScore.optional(),
  }),
});
export type ResultConfidence = z.infer<typeof ResultConfidence>;

export const Confidence = z.discriminatedUnion("level", [
  SignalConfidence,
  ThemeConfidence,
  ResultConfidence,
]);
export type Confidence = z.infer<typeof Confidence>;
