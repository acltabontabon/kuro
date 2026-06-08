import { z } from "zod";
import { Id } from "./primitives.js";
import { SignalConfidence } from "./confidence.js";

export const Sentiment = z.enum(["positive", "negative", "neutral", "mixed"]);
export type Sentiment = z.infer<typeof Sentiment>;

export const Signal = z.object({
  id: Id,
  topic: z.string().min(1),
  sentiment: Sentiment,
  claim: z.string().min(1),
  evidenceIds: z.array(Id).min(1),
  confidence: SignalConfidence,
});
export type Signal = z.infer<typeof Signal>;
