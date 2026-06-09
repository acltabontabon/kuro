import { z } from "zod";
import { Id, IsoDateTime } from "./primitives.js";

export const LocatorKind = z.enum(["charRange", "lineRange", "anchor"]);
export type LocatorKind = z.infer<typeof LocatorKind>;

export const Locator = z
  .discriminatedUnion("kind", [
    z.object({
      kind: z.literal("charRange"),
      start: z.number().int().min(0),
      end: z.number().int().min(0),
    }),
    z.object({
      kind: z.literal("lineRange"),
      startLine: z.number().int().min(1),
      endLine: z.number().int().min(1),
    }),
    z.object({
      kind: z.literal("anchor"),
      value: z.string().min(1),
    }),
  ])
  .superRefine((l, ctx) => {
    if (l.kind === "charRange" && l.end < l.start) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["end"],
        message: "Locator charRange end must be >= start",
      });
    }
    if (l.kind === "lineRange" && l.endLine < l.startLine) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["endLine"],
        message: "Locator lineRange endLine must be >= startLine",
      });
    }
  });
export type Locator = z.infer<typeof Locator>;

export const ExtractionMethod = z.enum(["verbatim", "normalized", "synthesized"]);
export type ExtractionMethod = z.infer<typeof ExtractionMethod>;

export const Extraction = z.object({
  method: ExtractionMethod,
  extractedAt: IsoDateTime,
  extractor: z.string().min(1),
});
export type Extraction = z.infer<typeof Extraction>;

export const SourceTrust = z.enum(["low", "medium", "high"]);
export type SourceTrust = z.infer<typeof SourceTrust>;

export const QualityHints = z.object({
  sourceTrust: SourceTrust.optional(),
  isDuplicateOf: Id.optional(),
  notes: z.string().min(1).optional(),
});
export type QualityHints = z.infer<typeof QualityHints>;

export const Evidence = z
  .object({
    id: Id,
    sourceDocumentId: Id,
    snippet: z.string().min(1),
    originalSnippet: z.string().min(1).optional(),
    locator: Locator,
    extraction: Extraction,
    qualityHints: QualityHints.optional(),
  })
  .superRefine((e, ctx) => {
    if (e.extraction.method === "synthesized") {
      const hasOriginal = typeof e.originalSnippet === "string" && e.originalSnippet.length > 0;
      const hasNote = typeof e.qualityHints?.notes === "string" && e.qualityHints.notes.length > 0;
      if (!hasOriginal && !hasNote) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["originalSnippet"],
          message:
            'Evidence with extraction.method "synthesized" requires originalSnippet or qualityHints.notes explaining why no verbatim anchor exists.',
        });
      }
    }
  });
export type Evidence = z.infer<typeof Evidence>;
