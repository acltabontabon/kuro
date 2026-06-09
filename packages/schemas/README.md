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
- **Data sufficiency status — discriminated union.** `KuroResult` is a `z.discriminatedUnion("dataSufficiency", […])` over four arms: `sufficient`, `partial`, `insufficient`, and `unsupported_category`. The arm itself decides which fields are legal: e.g. `themes` / `signals` / `evidence` / `inference` are absent from the `insufficient` and `unsupported_category` arms (the schema marks both arms `.strict()` so unknown keys are rejected). `sufficient` requires non-empty themes / signals / evidence / sourceDocuments. `partial` requires a non-empty `evidenceGaps` and caps confidence at `low` / `medium`. `insufficient` requires `insufficientDataReason` and a non-empty `suggestedNextSources` and caps confidence at `low` / `unknown`. `unsupported_category` carries `requestedCategory`, the MVP `supportedCategories`, and a `refusalMessage` — and nothing evidence-shaped at all. See [`../../docs/INSUFFICIENT_DATA.md`](../../docs/INSUFFICIENT_DATA.md) for the canonical rulebook.
- **Traceability.** A `KuroResult` embeds its own `sourceDocuments`, `evidence`, `signals`, and `themes` (no duplicate ids), and a `superRefine` rejects any document where:
  - a `Signal.evidenceIds` references missing evidence,
  - an `Evidence.sourceDocumentId` references a missing source,
  - a `Theme.signalIds` references missing signals, or
  - an `Inference.{patterns,consensus,disagreements,maySuggest,mayNotSuggest}[].themeIds` references missing themes.

A `KuroResult` that breaks the chain at any point will not parse.

## Examples

- [`examples/employer-acme.json`](examples/employer-acme.json) — Acme Corp as an employer (`dataSufficiency: sufficient`).
- [`examples/rental-123-main.json`](examples/rental-123-main.json) — 123 Main St, Apt 4B as a rental (`dataSufficiency: sufficient`, with explicit disagreement on noise — fixture #4 from the spec).
- [`examples/insufficient-data.json`](examples/insufficient-data.json) — `dataSufficiency: insufficient` with read-but-excluded sources, `sourceCoverage` explaining why each was unusable, and `suggestedNextSources` (fixture #3).
- [`examples/employment-no-sources.json`](examples/employment-no-sources.json) — `dataSufficiency: insufficient` for an employer with no Source Documents located (fixture #1).
- [`examples/employment-partial.json`](examples/employment-partial.json) — `dataSufficiency: partial` for an employer where only compensation comments exist, with `evidenceGaps` for the missing topic axes (fixture #2).
- [`examples/rental-unusable-sources.json`](examples/rental-unusable-sources.json) — `dataSufficiency: insufficient` for a rental with promotional + off-subject sources only.
- [`examples/result.invalid-category.json`](examples/result.invalid-category.json) — `dataSufficiency: unsupported_category` scope refusal (fixture #5).

## Validate

```sh
pnpm install
pnpm --filter @kuro/schemas typecheck
pnpm --filter @kuro/schemas validate
```

`validate` parses all example files with `KuroResult` and runs negative tests covering each traceability invariant; the per-`dataSufficiency` rules (empty themes / sourceDocuments / confidence=`unknown` on `sufficient`; missing `evidenceGaps` and forbidden `high` / `unknown` ratings on `partial`; missing `insufficientDataReason` / `suggestedNextSources` and forbidden `medium` / `high` ratings on `insufficient`; `insufficient` carrying themes / signals / evidence; `unsupported_category` with a supported requested category or an incomplete supported list); the Result breadth cap (`rating: "high"` requires ≥ 3 themes at `medium+`); the required `reasons` field at every confidence level; the rejection of `rating: "unknown"` at Signal and Theme levels; and `ThemeClaim` strictness.
