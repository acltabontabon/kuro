import { z } from "zod";
import { Id, IsoDateTime } from "./primitives.js";
import { Subject } from "./subject.js";
import { SourceDocument } from "./sourceDocument.js";
import { SourceAttribution } from "./sourceAttribution.js";
import { Evidence } from "./evidence.js";
import { Signal } from "./signal.js";
import { Theme } from "./theme.js";
import { KuroInference } from "./inference.js";
import { ResultConfidence } from "./confidence.js";
import { SourceSummary } from "./sourceSummary.js";
import { DecisionCategory } from "./decisionCategory.js";

/**
 * Every KURO Result declares exactly one ResultStatus. The status
 * is a first-class part of the result shape, never a disclaimer
 * appended to a normal-looking output. The schema below uses a
 * discriminated union on `dataSufficiency` so illegal shapes for
 * a given status are unrepresentable, not merely rejected at runtime.
 *
 * See docs/INSUFFICIENT_DATA.md for the canonical rules.
 */
export const ResultStatus = z.enum([
  "sufficient",
  "partial",
  "insufficient",
  "unsupported_category",
]);
export type ResultStatus = z.infer<typeof ResultStatus>;

/**
 * Why a Result is `insufficient`. Distinguishes "no sources found"
 * from "sources found but unusable" so the caller can react sensibly
 * without parsing prose.
 */
export const InsufficientDataReasonKind = z.enum([
  "no_sources_found",
  "no_usable_evidence",
  "subject_unidentifiable",
  "out_of_window",
  "other",
]);
export type InsufficientDataReasonKind = z.infer<typeof InsufficientDataReasonKind>;

export const InsufficientDataReason = z
  .object({
    kind: InsufficientDataReasonKind,
    explanation: z.string().min(1),
  })
  .strict();
export type InsufficientDataReason = z.infer<typeof InsufficientDataReason>;

/**
 * A concrete pointer to the kind of source that, if available, would
 * raise the result above insufficiency. Must be concrete (e.g.
 * "verified tenant reviews on PropertyReviewSite"), not generic advice
 * ("search more").
 */
export const SuggestedNextSource = z
  .object({
    sourceType: z.string().min(3),
    rationale: z.string().min(1),
  })
  .strict();
export type SuggestedNextSource = z.infer<typeof SuggestedNextSource>;

export const SourceCoverageAssessment = z.enum([
  "spam",
  "duplicate",
  "inaccessible",
  "unrelated",
  "too_vague",
  "not_about_subject",
  "stale",
  "promotional",
  "other",
]);
export type SourceCoverageAssessment = z.infer<typeof SourceCoverageAssessment>;

export const SourceCoverageEntry = z
  .object({
    sourceDocumentId: Id,
    assessment: SourceCoverageAssessment,
    note: z.string().min(1).optional(),
  })
  .strict();
export type SourceCoverageEntry = z.infer<typeof SourceCoverageEntry>;

/**
 * A topic area for which evidence is missing on a `partial` result.
 * Required (non-empty) on `partial` so callers can see exactly what
 * is narrow about the coverage.
 */
export const EvidenceGap = z
  .object({
    topic: z.string().min(1),
    note: z.string().min(1),
  })
  .strict();
export type EvidenceGap = z.infer<typeof EvidenceGap>;

const BaseFields = {
  id: Id,
  subject: Subject,
  generatedAt: IsoDateTime,
  limitations: z.array(z.string().min(1)).optional(),
} as const;

const SufficientResult = z.object({
  ...BaseFields,
  dataSufficiency: z.literal("sufficient"),
  category: DecisionCategory,
  summary: z.string().min(1),
  sourceDocuments: z.array(SourceDocument).min(1),
  sourceAttributions: z.array(SourceAttribution).min(1),
  evidence: z.array(Evidence).min(1),
  signals: z.array(Signal).min(1),
  themes: z.array(Theme).min(1),
  inference: KuroInference,
  sourceSummary: SourceSummary,
  confidence: ResultConfidence,
  finalKuro: z.string().min(1),
});

const PartialResult = z.object({
  ...BaseFields,
  dataSufficiency: z.literal("partial"),
  category: DecisionCategory,
  summary: z.string().min(1),
  sourceDocuments: z.array(SourceDocument).min(1),
  sourceAttributions: z.array(SourceAttribution).min(1),
  evidence: z.array(Evidence).min(1),
  signals: z.array(Signal).min(1),
  themes: z.array(Theme).min(1),
  inference: KuroInference,
  sourceSummary: SourceSummary,
  confidence: ResultConfidence,
  finalKuro: z.string().min(1),
  evidenceGaps: z.array(EvidenceGap).min(1),
});

const InsufficientResult = z
  .object({
    ...BaseFields,
    dataSufficiency: z.literal("insufficient"),
    category: DecisionCategory,
    summary: z.string().min(1),
    sourceDocuments: z.array(SourceDocument).default([]),
    sourceAttributions: z.array(SourceAttribution).default([]),
    sourceSummary: SourceSummary.optional(),
    sourceCoverage: z.array(SourceCoverageEntry).optional(),
    insufficientDataReason: InsufficientDataReason,
    suggestedNextSources: z.array(SuggestedNextSource).min(1),
    confidence: ResultConfidence,
    finalKuro: z.string().min(1),
  })
  .strict();

/**
 * Scope refusal. `requestedCategory` is a free string because by definition
 * it falls outside `DecisionCategory`'s enum.
 */
const UnsupportedCategoryResult = z
  .object({
    ...BaseFields,
    dataSufficiency: z.literal("unsupported_category"),
    requestedCategory: z.string().min(1),
    supportedCategories: z.array(DecisionCategory).min(1),
    refusalMessage: z.string().min(1),
  })
  .strict();

export const KuroResult = z
  .discriminatedUnion("dataSufficiency", [
    SufficientResult,
    PartialResult,
    InsufficientResult,
    UnsupportedCategoryResult,
  ])
  .superRefine((r, ctx) => {
    if (r.dataSufficiency === "unsupported_category") {
      if (DecisionCategory.options.includes(r.requestedCategory as DecisionCategory)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["requestedCategory"],
          message: `requestedCategory "${r.requestedCategory}" is in fact a supported MVP category; use a different dataSufficiency status`,
        });
      }
      const expected = [...DecisionCategory.options].sort();
      const got = [...new Set(r.supportedCategories)].sort();
      if (expected.length !== got.length || expected.some((c, i) => c !== got[i])) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["supportedCategories"],
          message: `supportedCategories must list exactly the MVP categories: ${expected.join(", ")}`,
        });
      }
      return;
    }

    // sufficient | partial | insufficient share evidence-like fields (or in
    // insufficient's case, possibly-empty source slabs). Validate referential
    // integrity for the ones that carry full evidence chains.
    const reportDupes = (
      arr: { id: string }[],
      key: "sourceDocuments" | "sourceAttributions" | "evidence" | "signals" | "themes",
    ) => {
      const seen = new Map<string, number>();
      arr.forEach((item, i) => {
        const prior = seen.get(item.id);
        if (prior !== undefined) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: [key, i, "id"],
            message: `Duplicate ${key} id "${item.id}" (also at index ${prior})`,
          });
        } else {
          seen.set(item.id, i);
        }
      });
    };

    const sourceDocs = r.sourceDocuments;
    const attributions = r.sourceAttributions;
    reportDupes(sourceDocs, "sourceDocuments");
    reportDupes(attributions, "sourceAttributions");

    const sourceIds = new Set(sourceDocs.map((s) => s.id));
    const attributionBySource = new Map<string, number>();
    attributions.forEach((a, i) => {
      if (!sourceIds.has(a.sourceDocumentId)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["sourceAttributions", i, "sourceDocumentId"],
          message: `SourceAttribution ${a.id} references unknown sourceDocumentId ${a.sourceDocumentId}`,
        });
        return;
      }
      const prior = attributionBySource.get(a.sourceDocumentId);
      if (prior !== undefined) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["sourceAttributions", i, "sourceDocumentId"],
          message: `sourceDocumentId ${a.sourceDocumentId} already attributed by sourceAttributions[${prior}]; attribution is 1:1 per Source Document.`,
        });
      } else {
        attributionBySource.set(a.sourceDocumentId, i);
      }
    });

    if (r.dataSufficiency === "insufficient") {
      if (r.confidence.rating !== "low" && r.confidence.rating !== "unknown") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["confidence", "rating"],
          message: `dataSufficiency "insufficient" requires confidence.rating "low" or "unknown", got "${r.confidence.rating}"`,
        });
      }
      r.sourceCoverage?.forEach((c, i) => {
        if (!sourceIds.has(c.sourceDocumentId)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["sourceCoverage", i, "sourceDocumentId"],
            message: `sourceCoverage[${i}] references unknown sourceDocumentId ${c.sourceDocumentId}`,
          });
        }
      });
      if (r.insufficientDataReason.kind === "no_sources_found" && sourceDocs.length > 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["insufficientDataReason", "kind"],
          message: `insufficientDataReason.kind "no_sources_found" is inconsistent with non-empty sourceDocuments`,
        });
      }
      return;
    }

    // sufficient | partial — share full evidence integrity checks
    reportDupes(r.evidence, "evidence");
    reportDupes(r.signals, "signals");
    reportDupes(r.themes, "themes");

    const evidenceIds = new Set(r.evidence.map((e) => e.id));
    const signalIds = new Set(r.signals.map((s) => s.id));
    const themeIds = new Set(r.themes.map((t) => t.id));

    r.evidence.forEach((e, i) => {
      if (!sourceIds.has(e.sourceDocumentId)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["evidence", i, "sourceDocumentId"],
          message: `Evidence ${e.id} references unknown sourceDocumentId ${e.sourceDocumentId}`,
        });
      }
    });

    r.signals.forEach((s, i) => {
      s.evidenceIds.forEach((eid, j) => {
        if (!evidenceIds.has(eid)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["signals", i, "evidenceIds", j],
            message: `Signal ${s.id} references unknown evidenceId ${eid}`,
          });
        }
      });
    });

    const locatorKey = (
      sourceDocumentId: string,
      locator: { kind: string } & Record<string, unknown>,
    ) => {
      switch (locator.kind) {
        case "charRange":
          return `${sourceDocumentId}|charRange|${locator.start}|${locator.end}`;
        case "lineRange":
          return `${sourceDocumentId}|lineRange|${locator.startLine}|${locator.endLine}`;
        case "anchor":
          return `${sourceDocumentId}|anchor|${locator.value}`;
        default:
          return `${sourceDocumentId}|${locator.kind}|${JSON.stringify(locator)}`;
      }
    };
    const seenLocators = new Map<string, number>();
    r.evidence.forEach((e, i) => {
      const key = locatorKey(
        e.sourceDocumentId,
        e.locator as { kind: string } & Record<string, unknown>,
      );
      const prior = seenLocators.get(key);
      const isMarkedDuplicate = typeof e.qualityHints?.isDuplicateOf === "string";
      if (prior !== undefined && !isMarkedDuplicate) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["evidence", i, "locator"],
          message: `Evidence ${e.id} duplicates the locator of evidence at index ${prior} on sourceDocument ${e.sourceDocumentId}; mark one with qualityHints.isDuplicateOf to retain both.`,
        });
      } else if (prior === undefined) {
        seenLocators.set(key, i);
      }
    });

    r.evidence.forEach((e, i) => {
      const dupId = e.qualityHints?.isDuplicateOf;
      if (dupId !== undefined && !evidenceIds.has(dupId)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["evidence", i, "qualityHints", "isDuplicateOf"],
          message: `Evidence ${e.id} qualityHints.isDuplicateOf references unknown evidenceId ${dupId}`,
        });
      }
      if (dupId !== undefined && dupId === e.id) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["evidence", i, "qualityHints", "isDuplicateOf"],
          message: `Evidence ${e.id} qualityHints.isDuplicateOf cannot reference itself`,
        });
      }
    });

    r.themes.forEach((t, i) => {
      t.signalIds.forEach((sid, j) => {
        if (!signalIds.has(sid)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["themes", i, "signalIds", j],
            message: `Theme ${t.id} references unknown signalId ${sid}`,
          });
        }
      });
    });

    const checkInferenceClaims = (
      key: "patterns" | "consensus" | "disagreements" | "maySuggest" | "mayNotSuggest",
    ) => {
      r.inference[key].forEach((claim, i) => {
        claim.themeIds.forEach((tid, j) => {
          if (!themeIds.has(tid)) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              path: ["inference", key, i, "themeIds", j],
              message: `Inference ${key}[${i}] references unknown themeId ${tid}`,
            });
          }
        });
      });
    };
    checkInferenceClaims("patterns");
    checkInferenceClaims("consensus");
    checkInferenceClaims("disagreements");
    checkInferenceClaims("maySuggest");
    checkInferenceClaims("mayNotSuggest");

    sourceDocs.forEach((s, i) => {
      if (!attributionBySource.has(s.id)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["sourceAttributions"],
          message: `dataSufficiency "${r.dataSufficiency}" requires a SourceAttribution for sourceDocuments[${i}] (id=${s.id})`,
        });
      }
    });

    if (r.dataSufficiency === "partial") {
      if (r.confidence.rating !== "low" && r.confidence.rating !== "medium") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["confidence", "rating"],
          message: `dataSufficiency "partial" requires confidence.rating "low" or "medium", got "${r.confidence.rating}"`,
        });
      }
      if (r.inference.limitations.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["inference", "limitations"],
          message: `dataSufficiency "partial" requires at least one inference.limitations entry`,
        });
      }
    }

    if (r.dataSufficiency === "sufficient") {
      if (r.confidence.rating === "unknown") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["confidence", "rating"],
          message: `dataSufficiency "sufficient" forbids confidence.rating "unknown"`,
        });
      }
      if (r.confidence.rating === "high") {
        const supporting = r.themes.filter((t) => t.confidence.rating !== "low").length;
        if (supporting < 3) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["confidence", "rating"],
            message: `Result rating "high" requires at least 3 themes with confidence rating "medium" or "high"; found ${supporting}.`,
          });
        }
      }
    }
  });

export type KuroResult = z.infer<typeof KuroResult>;
export type SufficientKuroResult = z.infer<typeof SufficientResult>;
export type PartialKuroResult = z.infer<typeof PartialResult>;
export type InsufficientKuroResult = z.infer<typeof InsufficientResult>;
export type UnsupportedCategoryKuroResult = z.infer<typeof UnsupportedCategoryResult>;
