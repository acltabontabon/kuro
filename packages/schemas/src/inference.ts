import { z } from "zod";
import { Id } from "./primitives.js";

export const InferenceClaim = z.object({
  description: z.string().min(1),
  themeIds: z.array(Id).min(1),
});
export type InferenceClaim = z.infer<typeof InferenceClaim>;

export const KuroInference = z.object({
  patterns: z.array(InferenceClaim),
  consensus: z.array(InferenceClaim),
  disagreements: z.array(InferenceClaim),
  communitySentimentSummary: z.string().min(1),
  maySuggest: z.array(InferenceClaim),
  mayNotSuggest: z.array(InferenceClaim),
  limitations: z.array(z.string().min(1)),
});
export type KuroInference = z.infer<typeof KuroInference>;

export const signalSummary = (i: KuroInference): string => i.communitySentimentSummary;
