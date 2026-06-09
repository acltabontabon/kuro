import { z } from "zod";
import { Id, IsoDateTime, Url } from "./primitives.js";

export const SourceType = z.enum([
  "review_site",
  "forum",
  "social_media",
  "blog",
  "news",
  "company_site",
  "job_board",
  "documentation",
  "other",
]);
export type SourceType = z.infer<typeof SourceType>;

export const TrustTier = z.enum([
  "primary",
  "secondary",
  "community",
  "low_context",
  "unknown",
]);
export type TrustTier = z.infer<typeof TrustTier>;

export const AccessedVia = z.enum([
  "direct_fetch",
  "user_paste",
  "file_upload",
  "api_import",
  "other",
]);
export type AccessedVia = z.infer<typeof AccessedVia>;

export const RedactionCategory = z.enum([
  "pii",
  "private_id",
  "email",
  "real_name",
  "hidden_metadata",
  "other",
]);
export type RedactionCategory = z.infer<typeof RedactionCategory>;

export const RedactionRecord = z
  .object({
    field: z.string().min(1),
    category: RedactionCategory,
    reason: z.string().min(1).max(500).optional(),
  })
  .strict();
export type RedactionRecord = z.infer<typeof RedactionRecord>;

const MetadataPrimitive = z.union([
  z.string(),
  z.number(),
  z.boolean(),
  z.null(),
]);

export const AttributionMetadata = z
  .record(z.string().min(1), MetadataPrimitive)
  .refine((m) => Object.keys(m).length <= 20, {
    message: "AttributionMetadata may contain at most 20 keys",
  });
export type AttributionMetadata = z.infer<typeof AttributionMetadata>;

const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const REAL_NAME_TOKEN = /^[A-Z][a-z'’\-]+$/;
const HANDLE_MARKERS = /[@_0-9]/;
const TRACKING_PARAM_KEYS = [
  "utm_source",
  "utm_medium",
  "utm_campaign",
  "utm_term",
  "utm_content",
  "fbclid",
  "gclid",
  "mc_cid",
  "mc_eid",
  "ref",
];

function looksLikeRealName(s: string): boolean {
  if (HANDLE_MARKERS.test(s)) return false;
  const tokens = s.trim().split(/\s+/);
  if (tokens.length < 2) return false;
  return tokens.every((t) => REAL_NAME_TOKEN.test(t));
}

function urlHasTrackingParam(raw: string): boolean {
  const queryStart = raw.indexOf("?");
  if (queryStart < 0) return false;
  const query = raw.slice(queryStart + 1).split("#")[0] ?? "";
  if (!query) return false;
  for (const pair of query.split("&")) {
    const eq = pair.indexOf("=");
    const key = eq < 0 ? pair : pair.slice(0, eq);
    if (TRACKING_PARAM_KEYS.includes(key)) return true;
  }
  return false;
}

export const SourceAttribution = z
  .object({
    id: Id,
    sourceDocumentId: Id,
    sourceType: SourceType,
    url: Url.optional(),
    canonicalUrl: Url.optional(),
    title: z.string().min(1).max(500).optional(),
    authorHandle: z.string().min(1).max(200).optional(),
    publishedAt: IsoDateTime.optional(),
    fetchedAt: IsoDateTime,
    accessedVia: AccessedVia.optional(),
    trustTier: TrustTier,
    trustRationale: z.string().min(1).max(1000).optional(),
    metadata: AttributionMetadata.optional(),
    redactions: z.array(RedactionRecord).optional(),
  })
  .superRefine((a, ctx) => {
    if (a.url === undefined && a.accessedVia === undefined) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["url"],
        message:
          "SourceAttribution requires either url or accessedVia (when url is absent).",
      });
    }

    const now = new Date().toISOString();
    if (a.fetchedAt > now) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["fetchedAt"],
        message: "fetchedAt must not be in the future.",
      });
    }

    if (
      a.publishedAt !== undefined &&
      a.publishedAt > a.fetchedAt &&
      (a.trustRationale === undefined || a.trustRationale.length === 0)
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["publishedAt"],
        message:
          "publishedAt is after fetchedAt; provide trustRationale to explain (e.g., clock skew, late edit).",
      });
    }

    if (a.trustTier === "unknown" && (a.trustRationale === undefined || a.trustRationale.length === 0)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["trustRationale"],
        message: 'trustTier "unknown" requires a non-empty trustRationale.',
      });
    }

    if (a.authorHandle !== undefined) {
      if (EMAIL_SHAPE.test(a.authorHandle)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["authorHandle"],
          message:
            "authorHandle must be a public handle, not an email address.",
        });
      } else if (looksLikeRealName(a.authorHandle)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["authorHandle"],
          message:
            "authorHandle must be a public platform handle, not a real-name shape.",
        });
      }
    }

    if (a.canonicalUrl !== undefined && urlHasTrackingParam(a.canonicalUrl)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["canonicalUrl"],
        message:
          "canonicalUrl must be normalized: known tracking parameters (utm_*, fbclid, gclid, ref, mc_cid, mc_eid) must be stripped.",
      });
    }
  });
export type SourceAttribution = z.infer<typeof SourceAttribution>;
