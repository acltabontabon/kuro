import { z } from "zod";
import { Id } from "./primitives.js";
import { Sentiment } from "./signal.js";
import { ThemeConfidence } from "./confidence.js";

export const Theme = z.object({
  id: Id,
  topic: z.string().min(1),
  sentiment: Sentiment,
  signalIds: z.array(Id).min(1),
  confidence: ThemeConfidence,
});
export type Theme = z.infer<typeof Theme>;
