import { z } from "zod";
import { Id, IsoDateTime } from "./primitives.js";
import { Subject } from "./subject.js";
import { SourceDocument } from "./sourceDocument.js";
import { Evidence } from "./evidence.js";
import { Signal } from "./signal.js";
import { Theme } from "./theme.js";
import { KuroInference } from "./inference.js";
import { ResultConfidence } from "./confidence.js";
import { SourceSummary } from "./sourceSummary.js";

export const ResultOutcome = z.enum(["ok", "insufficient_data"]);
export type ResultOutcome = z.infer<typeof ResultOutcome>;

export const KuroResult = z.object({
  id: Id,
  subject: Subject,
  generatedAt: IsoDateTime,
  outcome: ResultOutcome,
  summary: z.string().min(1),
  sourceDocuments: z.array(SourceDocument),
  evidence: z.array(Evidence),
  signals: z.array(Signal),
  themes: z.array(Theme),
  inference: KuroInference,
  sourceSummary: SourceSummary,
  confidence: ResultConfidence,
  finalKuro: z.string().min(1),
}).superRefine((r, ctx) => {
  const reportDupes = (key: "sourceDocuments" | "evidence" | "signals" | "themes") => {
    const seen = new Map<string, number>();
    (r[key] as { id: string }[]).forEach((item, i) => {
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
  reportDupes("sourceDocuments");
  reportDupes("evidence");
  reportDupes("signals");
  reportDupes("themes");

  const sourceIds = new Set(r.sourceDocuments.map((s) => s.id));
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

  if (r.outcome === "ok") {
    (
      [
        ["sourceDocuments", r.sourceDocuments.length],
        ["evidence", r.evidence.length],
        ["signals", r.signals.length],
        ["themes", r.themes.length],
      ] as const
    ).forEach(([key, len]) => {
      if (len === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: [key],
          message: `outcome "ok" requires at least one ${key} entry`,
        });
      }
    });
  } else {
    if (r.confidence.rating !== "low" && r.confidence.rating !== "unknown") {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["confidence", "rating"],
        message: `outcome "insufficient_data" requires confidence.rating of "low" or "unknown", got "${r.confidence.rating}"`,
      });
    }
    if (r.inference.limitations.length === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["inference", "limitations"],
        message: `outcome "insufficient_data" requires at least one inference.limitations entry`,
      });
    }
  }
});
export type KuroResult = z.infer<typeof KuroResult>;
