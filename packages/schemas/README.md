# @kuro/schemas

Zod schemas for the KURO domain model — the user-/API-facing shape of a `KuroResult` and every concept it composes (`Subject`, `SourceDocument`, `Evidence`, `Signal`, `Theme`, `KuroInference`, `Confidence`).

## Source of truth

Field semantics are governed by [`../../docs/GLOSSARY.md`](../../docs/GLOSSARY.md). If a concept here disagrees with the glossary, the glossary wins — update the glossary first, then this package. If the schema cannot represent a glossary concept without distortion, reshape the schema, not the concept.

This package is **not** the database DDL (tracked separately) and **not** the per-AI-call output contract (tracked separately). Those downstream artifacts should conform to these shapes where they overlap.

## What's structurally enforced

- **Topic-first organization.** A `KuroResult` only holds `themes: Theme[]`. There are no `positiveSignals` / `negativeSignals` arrays anywhere. Sentiment is an attribute of `Signal` and `Theme`, never an organizing partition.
- **Four-value `Sentiment`** — `positive | negative | neutral | mixed`, first-class at both `Signal` and `Theme`. `neutral` is for informational/non-emotional observations; `mixed` is for genuine internal tension. They are not interchangeable. Per the glossary, at the `Signal` level `mixed` is reserved for opinions that cannot be fairly split into separate `Signal`s.
- **Theme-level interpretation layer.** Every `Theme` carries `maySuggest: ThemeClaim[]`, `mayNotSuggest: ThemeClaim[]`, and `limitations: string[]`. `ThemeClaim` is a small `{ description }` schema and is `.strict()` — extra fields like `themeIds` are rejected, because the parent theme is the traceability boundary. There is intentionally no separate prose `interpretation` field; the structured trio covers it.
- **`KuroInference` may / may not.** The schema only exposes fields for what the glossary says an Inference *may* claim — `patterns`, `consensus`, `disagreements`, `communitySentimentSummary` — plus `maySuggest`, `mayNotSuggest`, and `limitations` to make the constraint first-class on the value. Cross-theme `InferenceClaim`s carry `themeIds`, so even a "may not" claim is tied back to the themes it is disclaiming over. There is no `verdict`, `recommendation`, `prediction`, `truthClaim`, `ranking`, or `decision` field. Do not add one without updating the glossary first.
- **`summary` vs `finalKuro`.** `KuroResult.summary` is a descriptive overview of what KURO *observed* (stays close to source-backed Signals). `KuroResult.finalKuro` is a cautious closing **synthesis** using may / may-not framing; it must not become advice.
- **`sourceSummary`.** Aggregated evidence-base description: `documentCount`, per-platform counts, exclusions (`{ reason, count }`), a `freshness` window (`{ oldestPublishedAt, newestPublishedAt }` or `null` when no publish dates are available), and `diversityNotes`. Gives the UI/API a single place to render "what was read."
- **Confidence describes support, not truth.** Each `Confidence` carries a qualitative `rating` plus a required `reasons: Array<{ driver, effect, note }>` (min 1, at every level — including `high`). The rating enum is **split by level**: `SubResultConfidenceRating = low | medium | high` for Signal and Theme, and `ResultConfidenceRating = low | medium | high | unknown` for Result. `unknown` is unrepresentable at Signal and Theme by construction. Every `Confidence` carries the four canonical glossary inputs (`sourceCount`, `sourceDiversity`, `sourceFreshness`, `signalConsistency`) and adds level-specific inputs on top. `supportScore` and `inputs` are marked `@internal` — they describe machinery, not the public API response. For insufficient-data Results, prefer `unknown` when no usable evidence exists at all and `low` when some evidence exists but is weak, narrow, stale, or contradictory. See [`../../docs/CONFIDENCE.md`](../../docs/CONFIDENCE.md) for the full MVP rules (bands, drivers, breadth cap, `reasons` contract, public-vs-internal exposure).
- **Insufficient-data outcome.** `KuroResult.outcome` is `"ok" | "insufficient_data"`. When `outcome === "ok"`, `sourceDocuments` / `evidence` / `signals` / `themes` must each be non-empty. When `outcome === "insufficient_data"`, they may be empty, but `confidence.rating` must be `low` or `unknown` and `inference.limitations` must be non-empty. Insufficient data is a successful Result, never a transport error.
- **Traceability.** A `KuroResult` embeds its own `sourceDocuments`, `evidence`, `signals`, and `themes` (no duplicate ids), and a `superRefine` rejects any document where:
  - a `Signal.evidenceIds` references missing evidence,
  - an `Evidence.sourceDocumentId` references a missing source,
  - a `Theme.signalIds` references missing signals, or
  - an `Inference.{patterns,consensus,disagreements,maySuggest,mayNotSuggest}[].themeIds` references missing themes.

A `KuroResult` that breaks the chain at any point will not parse.

## Examples

- [`examples/employer-acme.json`](examples/employer-acme.json) — Acme Corp as an employer (`outcome: ok`).
- [`examples/rental-123-main.json`](examples/rental-123-main.json) — 123 Main St, Apt 4B as a rental (`outcome: ok`).
- [`examples/insufficient-data.json`](examples/insufficient-data.json) — `outcome: insufficient_data` with read-but-excluded sources, empty themes/signals/evidence, `confidence.rating: unknown`, and non-empty `inference.limitations`.
- [`examples/mixed-sentiment-high.json`](examples/mixed-sentiment-high.json) — `outcome: ok` with a `sentiment: "mixed"` Theme at `confidence.rating: "high"`, illustrating that mixed sentiment does not lower confidence when each side internally agrees on its claims.

## Validate

```sh
pnpm install
pnpm --filter @kuro/schemas typecheck
pnpm --filter @kuro/schemas validate
```

`validate` parses all example files with `KuroResult` and runs a set of negative tests covering each traceability invariant, the `outcome`-conditional rules (empty themes on `ok`, high-rated confidence on `insufficient_data`, empty `inference.limitations` on `insufficient_data`), the Result breadth cap (`rating: "high"` requires ≥ 3 themes at `medium+`), the required `reasons` field at every confidence level, the rejection of `rating: "unknown"` at Signal and Theme levels, and `ThemeClaim` strictness.
