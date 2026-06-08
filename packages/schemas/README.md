# @kuro/schemas

Zod schemas for the KURO domain model — the user-/API-facing shape of a `KuroResult` and every concept it composes (`Subject`, `SourceDocument`, `Evidence`, `Signal`, `Theme`, `KuroInference`, `Confidence`).

## Source of truth

Field semantics are governed by [`../../docs/GLOSSARY.md`](../../docs/GLOSSARY.md). If a concept here disagrees with the glossary, the glossary wins — update the glossary first, then this package. If the schema cannot represent a glossary concept without distortion, reshape the schema, not the concept.

This package is **not** the database DDL (tracked separately) and **not** the per-AI-call output contract (tracked separately). Those downstream artifacts should conform to these shapes where they overlap.

## What's structurally enforced

- **Topic-first organization.** A `KuroResult` only holds `themes: Theme[]`. There are no `positiveSignals` / `negativeSignals` arrays anywhere. Sentiment is an attribute of `Signal` and `Theme`, never an organizing partition.
- **`mixed` is a first-class `Sentiment`** at both `Signal` and `Theme` levels. Per the glossary, at the `Signal` level it is reserved for genuinely ambivalent opinions; separable positive/negative content should yield separate `Signal`s.
- **`KuroInference` may / may not.** The schema only exposes fields for what the glossary says an Inference *may* claim — `patterns`, `consensus`, `disagreement`, `communitySentimentSummary` — plus `maySuggest`, `mayNotSuggest`, and `limitations` to make the constraint first-class on the value. There is no `verdict`, `recommendation`, `prediction`, `truthClaim`, `ranking`, or `decision` field. Do not add one without updating the glossary first.
- **Confidence describes support, not truth.** The numeric field is named `supportScore` (optional) and pairs with a qualitative `rating` of `low | medium | high | unknown` (required). Every `Confidence` carries the four canonical glossary inputs (`sourceCount`, `sourceDiversity`, `sourceFreshness`, `signalConsistency`) and adds level-specific inputs on top.
- **Traceability.** A `KuroResult` embeds its own `sourceDocuments`, `evidence`, `signals`, and `themes`, and a `superRefine` rejects any document where:
  - a `Signal.evidenceIds` references missing evidence,
  - an `Evidence.sourceDocumentId` references a missing source,
  - a `Theme.signalIds` references missing signals, or
  - an `Inference.{patterns,consensus,disagreement}[].themeIds` references missing themes.

A `KuroResult` that breaks the chain at any point will not parse.

## Examples

- [`examples/employer-acme.json`](examples/employer-acme.json) — Acme Corp as an employer.
- [`examples/rental-123-main.json`](examples/rental-123-main.json) — 123 Main St, Apt 4B as a rental.

## Validate

```sh
pnpm install
pnpm --filter @kuro/schemas typecheck
pnpm --filter @kuro/schemas validate
```

`validate` parses both example files with `KuroResult` and runs a small set of negative tests that intentionally break each traceability invariant to confirm the refinements fire.
