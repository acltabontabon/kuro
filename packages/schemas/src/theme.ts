import { z } from "zod";
import { Id } from "./primitives.js";
import { Sentiment } from "./signal.js";
import { ThemeConfidence } from "./confidence.js";

export const ThemeClaim = z.object({
  description: z.string().min(1),
}).strict();
export type ThemeClaim = z.infer<typeof ThemeClaim>;

export const Theme = z.object({
  id: Id,
  topic: z.string().min(1),
  sentiment: Sentiment,
  signalIds: z.array(Id).min(1),
  confidence: ThemeConfidence,
  maySuggest: z.array(ThemeClaim),
  mayNotSuggest: z.array(ThemeClaim),
  limitations: z.array(z.string().min(1)),
});
export type Theme = z.infer<typeof Theme>;
