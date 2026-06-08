import { z } from "zod";
import { Id, IsoDateTime } from "./primitives.js";
import { Subject } from "./subject.js";
import { SourceDocument } from "./sourceDocument.js";
import { Evidence } from "./evidence.js";
import { Signal } from "./signal.js";
import { Theme } from "./theme.js";
import { KuroInference } from "./inference.js";
import { ResultConfidence } from "./confidence.js";

export const KuroResult = z.object({
  id: Id,
  subject: Subject,
  generatedAt: IsoDateTime,
  sourceDocuments: z.array(SourceDocument),
  evidence: z.array(Evidence),
  signals: z.array(Signal),
  themes: z.array(Theme).min(1),
  inference: KuroInference,
  confidence: ResultConfidence,
}).superRefine((r, ctx) => {
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

  const checkInferenceClaims = (key: "patterns" | "consensus" | "disagreement") => {
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
  checkInferenceClaims("disagreement");
});
export type KuroResult = z.infer<typeof KuroResult>;
