# KURO Source Attribution Model (MVP)

This document is the rulebook for KURO's Source Attribution layer. The conceptual definition lives in [GLOSSARY.md §Source Attribution](./GLOSSARY.md#source-attribution); this file pins the field semantics, enums, validation rules, and the attribution ↔ Evidence ↔ Confidence boundary that the schema and ingestion paths must agree on.

Source Attribution requirements apply in full regardless of [Decision Category](./DECISION_CATEGORIES.md). Category bounds scope; it does not relax attribution, trust-tier, or redaction rules.

## 1. Definition

> **Source Attribution** is a structured record describing the origin of a Source Document: its type, location, collection context, and a coarse reliability hint. It is metadata about provenance — not a claim of truth, not an excerpt, not a confidence value.

Attribution answers **"Where did this material come from?"** It does not interpret the material, it does not quote it, and it does not weigh it.

> **Trust tier is a reliability hint, never a truth probability.**

## 2. Three layers, three questions

| Layer | Question |
|---|---|
| **Source Attribution** | Where did this material come from? |
| **Evidence** | Which exact excerpt supports this interpreted Signal? |
| **Confidence** | How strong is the support across the evidence KURO actually read? |

These three layers must not collapse into each other:

- Attribution is **not** Evidence. Attribution does not quote anything; it points at the source the quoted excerpt was lifted from. A Signal that "cites attribution" instead of Evidence is invalid by construction.
- Attribution is **not** Confidence. `trustTier` is a coarse provenance label (e.g. forum vs. official site), not a probability of correctness. It is **never** rendered as a truth label and must never feed directly into the Result-level truth claim.
- Attribution is **not** the Source Document itself. The Source Document carries the ingested material; Attribution carries the provenance metadata about it. They are 1:1 but separate records — separated so attribution can be rendered, audited, and redacted without rendering the full source.

## 3. Concept boundary

```
Source Document
    |  (excerpt extracted from)
    v                                 (described by, 1:1)
Evidence       <---- traced to ----   SourceDocument <---- SourceAttribution
    |  (interpreted as)
    v
Signal --> Theme --> KURO Inference --> KURO Result
```

Signals and Themes reach attribution **transitively** through `Evidence → SourceDocument → SourceAttribution`. They must not embed attribution payloads inline. The schema enforces this by giving SourceAttribution its own top-level array on `KuroResult` (no `attribution` field on Signal, Evidence, or Theme).

Cardinality:

- Source Document → Source Attribution: **1:1**. Every SourceDocument is described by exactly one SourceAttribution when `dataSufficiency` is `sufficient` or `partial`. (The schema enforces both directions: an attribution must point at a known SourceDocument id, and no two attributions may share one.)
- Source Attribution → Evidence: **0:N** indirectly, through the SourceDocument. Attribution carries no Evidence link of its own.

## 4. Fields

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Stable id for this attribution record. |
| `sourceDocumentId` | string | yes | FK to the SourceDocument this attribution describes. 1:1. |
| `sourceType` | enum | yes | Coarse publishing-surface category. See §5. |
| `url` | URL | conditional | Required unless `accessedVia` describes a non-URL origin. |
| `canonicalUrl` | URL | optional | Normalized form of `url`. Must be free of known tracking parameters. |
| `title` | string | optional | Public page/thread title, when available. |
| `authorHandle` | string | optional | Public handle only. Never an email, never a real-name shape. |
| `publishedAt` | ISO-8601 datetime | optional | Source-declared publish time. Never guessed. |
| `fetchedAt` | ISO-8601 datetime | yes | When KURO obtained the material. Must not be in the future. |
| `accessedVia` | enum | conditional | Required when `url` is absent. See §5. |
| `trustTier` | enum | yes | Coarse reliability hint. See §5. |
| `trustRationale` | string | conditional | Required when `trustTier: "unknown"`; recommended for `low_context`. |
| `metadata` | object | optional | Bounded record (≤20 keys, primitive values only). |
| `redactions` | array | optional | What was removed and why, by category. Never the raw value. |

## 5. Enums

### `sourceType`

`review_site` · `forum` · `social_media` · `blog` · `news` · `company_site` · `job_board` · `documentation` · `other`

Used for UI rendering and interpretation hints. **Not** a truth signal.

### `trustTier`

| Value | Meaning |
|---|---|
| `primary` | Source is close to the originating entity or official publisher. |
| `secondary` | Source is curated or structured but not the originating authority. |
| `community` | Public community discussion or forum-style first-hand account. |
| `low_context` | Source has limited surrounding context (e.g. pasted text). |
| `unknown` | Provenance is unclear or cannot be safely classified. Requires `trustRationale`. |

`trustTier` is a coarse provenance hint. It is **not** a probability of correctness and **must not** substitute for Evidence or Confidence. Any UI that surfaces it must frame it as a reliability hint, never a truth label.

### `accessedVia`

`direct_fetch` · `user_paste` · `file_upload` · `api_import` · `other`

Used when there is no meaningful URL — e.g. excerpts the user pasted directly, files the user uploaded, content imported via an API.

### `RedactionRecord.category`

`pii` · `private_id` · `email` · `real_name` · `hidden_metadata` · `other`

A `RedactionRecord` records **that a field was removed and why**. It carries `field`, `category`, and an optional `reason`. It **never** carries the raw value that was removed; the strict shape rejects a `value` key at validation.

## 6. Rules

These rules are encoded in the schema, not only here.

1. **`fetchedAt` is required.** Every attribution must record when KURO obtained the material. It must not be in the future.
2. **`url` or `accessedVia`.** Either `url` is present, or `accessedVia` is present. If both are absent the record is rejected.
3. **`publishedAt ≤ fetchedAt`** unless `trustRationale` explains the exception (e.g. clock skew, late edit timestamps).
4. **`publishedAt` is never inferred or guessed.** Omit if unreliable.
5. **`authorHandle` is public-only.** Email-shaped values are rejected at validation. Real-name shapes (multiple whitespace-separated tokens, each starting with a capital letter, no handle markers like `@`, `_`, or digits) are rejected. Private identifiers, profile IDs, and hidden platform metadata must never be stored as attribution.
6. **`canonicalUrl`** must be normalized when present: tracking parameters (`utm_*`, `fbclid`, `gclid`, `ref`, `mc_cid`, `mc_eid`) must be stripped. Records that still carry these are rejected. Omit `canonicalUrl` when safe normalization is not possible.
7. **`trustTier: "unknown"` requires a non-empty `trustRationale`.** A coarse "I don't know" must always be reasoned.
8. **`metadata` is bounded.** Maximum 20 keys; values must be string, number, boolean, or null. No nested objects, no arrays, no raw page body, no scraped HTML, no full comments, no private payloads. `metadata` is for short, non-PII collection hints (HTTP status, content type, language hint).
9. **Redactions never carry the raw value.** `RedactionRecord` is `.strict()` and a key named `value` fails validation. The schema preserves the *category* of what was removed; it never preserves the value.
10. **Attribution is 1:1 per SourceDocument.** Two attributions referencing the same `sourceDocumentId` are rejected.
11. **`sufficient` and `partial` Results require attribution for every SourceDocument.** When the Result claims to have read material, every read document must carry an attribution record. On `insufficient` Results that retrieved any documents, attributions for those documents are likewise expected.
12. **Attribution never replaces Evidence.** Signals still require Evidence spans rooted in a SourceDocument. A Signal cannot cite attribution.
13. **Trust tier never feeds the Result truth claim.** It is a UI hint and a debugging label, not an input to "is this true."

## 7. Output behavior

How attribution surfaces in user/API-facing `KuroResult`:

- **`SourceSummary` summarizes the source mix** via `sourceTypes` and `trustTiers` counts. The Result does not enumerate every URL inline.
- **Themes and Signals reach attribution transitively.** They must not duplicate full attribution payloads at the Signal level.
- **UI may show:** `sourceType`, `title` or domain, `fetchedAt`, and `authorHandle` when public and safe.
- **UI should avoid rendering raw URLs on every Signal.** The raw URL belongs in an expanded / inspector view.
- **Any rendering of `trustTier` must be paired with language framing it as a reliability hint, never a truth label.**

## 8. Examples

### 8.1 Public review-site page

```json
{
  "id": "att_glassdoor_1",
  "sourceDocumentId": "src_glassdoor_1",
  "sourceType": "review_site",
  "url": "https://www.glassdoor.com/Reviews/acme-corp-RVW9001.htm",
  "canonicalUrl": "https://www.glassdoor.com/Reviews/acme-corp-RVW9001.htm",
  "title": "Acme Corp Reviews",
  "publishedAt": "2026-02-12T00:00:00Z",
  "fetchedAt": "2026-05-20T09:05:00Z",
  "accessedVia": "direct_fetch",
  "trustTier": "secondary"
}
```

### 8.2 Forum thread with public author handle

```json
{
  "id": "att_reddit_1",
  "sourceDocumentId": "src_reddit_1",
  "sourceType": "forum",
  "url": "https://www.reddit.com/r/jobs/comments/abc123/two_years_at_acme",
  "title": "Two years at Acme - honest review",
  "authorHandle": "throwaway_acme_42",
  "publishedAt": "2025-11-04T18:22:00Z",
  "fetchedAt": "2026-05-20T09:00:00Z",
  "accessedVia": "direct_fetch",
  "trustTier": "community",
  "metadata": { "httpStatus": 200, "languageHint": "en" }
}
```

`authorHandle` is the platform handle, not a real name. Email-shaped or real-name-shaped values are rejected at validation.

### 8.3 User-pasted excerpt (no URL)

```json
{
  "id": "att_paste_1",
  "sourceDocumentId": "src_paste_1",
  "sourceType": "other",
  "fetchedAt": "2026-06-01T10:00:00Z",
  "accessedVia": "user_paste",
  "trustTier": "low_context",
  "trustRationale": "Excerpt was pasted by the user without surrounding source context.",
  "redactions": [
    { "field": "authorHandle", "category": "real_name", "reason": "User-supplied byline appeared to be a real name." }
  ]
}
```

No `url`. The non-URL origin is recorded with `accessedVia: user_paste`. The `redactions` array records that an apparent real-name byline was removed — without storing the value.

## 9. Non-goals

- **No crawler.** This model does not build or schedule fetching infrastructure.
- **No scraping logic.** Extraction strategies are out of scope.
- **No source ranking algorithm.** Trust tier is a coarse hint, not a learned score.
- **No legal or compliance labeling.** Attribution is not a license/permission claim.
- **No private identity exposure.** Real names, emails, internal IDs, and hidden platform metadata stay out.
- **Trust tier is not a truth probability.** It must never be used or rendered as one.
- **Evidence is not optional.** Attribution does not replace the requirement that Signals cite Evidence.

## 10. Cross-links

- Conceptual definition: [GLOSSARY.md §Source Attribution](./GLOSSARY.md#source-attribution).
- Evidence layer and the "no Evidence, no Signal" rule: [EVIDENCE.md](./EVIDENCE.md).
- Confidence model that consumes Evidence strength, separate from attribution: [CONFIDENCE.md](./CONFIDENCE.md).
- Schema: [`packages/schemas/src/sourceAttribution.ts`](../packages/schemas/src/sourceAttribution.ts) and the attribution-related validation rules in [`packages/schemas/src/result.ts`](../packages/schemas/src/result.ts).
