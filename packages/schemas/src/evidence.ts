import { z } from "zod";
import { Id } from "./primitives.js";

export const EvidencePosition = z.object({
  start: z.number().int().min(0),
  end: z.number().int().min(0),
}).refine((p) => p.end >= p.start, {
  message: "Evidence position end must be >= start",
});
export type EvidencePosition = z.infer<typeof EvidencePosition>;

export const Evidence = z.object({
  id: Id,
  sourceDocumentId: Id,
  excerpt: z.string().min(1),
  position: EvidencePosition,
  surroundingContext: z.string().optional(),
});
export type Evidence = z.infer<typeof Evidence>;
