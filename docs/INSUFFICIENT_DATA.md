# KURO Insufficient-Data Behavior

This document is the canonical rulebook for how KURO behaves when the available evidence is too thin, too narrow, or out of scope to support a normal cautious result. The contract here is binding: schemas, prompts, pipelines, and surfaces must conform.

The defining commitment: **insufficient data is a first-class result state, not a disclaimer appended to a normal-looking output**. KURO never fabricates themes, signals, confidence, or summaries to fill gaps.

See [GLOSSARY.md](./GLOSSARY.md) for term definitions, [CONFIDENCE.md](./CONFIDENCE.md) for confidence rules, and [DECISION_CATEGORIES.md](./DECISION_CATEGORIES.md) for scope.

## 1. What "insufficient data" means

A KURO Result is *insufficient* when there is no body of usable Evidence over which a meaningful inference can be drawn. This is not a transport failure. The request succeeded; the analytical answer is "we cannot responsibly say."

KURO refuses to conclude in this branch because:

- A conclusion drawn from no usable Evidence violates the **No Evidence, no Signal** invariant.
- A conclusion drawn from material KURO has not actually read replaces lived community experience with generic likelihood — exactly the inversion KURO exists to avoid.
- A "low-confidence guess" looks the same shape as a high-confidence finding and would be read that way by callers and end users.

## 2. The four statuses

Every Result declares exactly one `dataSufficiency`:

| Status | Meaning |
|---|---|
| `sufficient` | Enough usable evidence for a normal cautious KURO result. |
| `partial` | Some usable evidence; only narrow observations are supported. |
| `insufficient` | No meaningful inference can be supported. |
| `unsupported_category` | Request is outside KURO's MVP scope. |

These statuses are mutually exclusive. The schema represents the Result as a **discriminated union** on `dataSufficiency`, so illegal shapes for a given status are unrepresentable — not merely rejected at runtime. (See §6 *Discriminated-union representation*.)

## 3. When each status applies

### `sufficient`
Usable Evidence is broad enough to support themes and an inference at a normal cautious quality. Confidence may be `low`, `medium`, or `high` subject to the breadth cap defined in [CONFIDENCE.md §5](./CONFIDENCE.md). `unknown` is forbidden on `sufficient`.

### `partial`
Some usable Evidence exists, but coverage is narrow — typically a single topic axis, a single platform, or a single time slice. The Result returns only what the Evidence directly supports. It must:

- Include `evidenceGaps` (non-empty), naming topics for which Evidence is missing.
- Cap confidence at `low` or `medium`.
- Avoid any broad final conclusion or unsupported overall sentiment.
- Carry at least one Inference `limitations` entry.

`partial` is the right answer when KURO has *some* signal to offer but is not in a position to render a full picture.

### `insufficient`
KURO cannot support any meaningful inference. This branch applies when:

- **No sources were found** (`insufficientDataReason.kind = "no_sources_found"`): `sourceDocuments` must be empty.
- **Sources were found but no usable Evidence** (`kind = "no_usable_evidence"`): include `sourceCoverage` so the caller can see *why* each document failed.
- **Subject is unidentifiable** (`kind = "subject_unidentifiable"`): KURO could not decide whether retrieved material was about the named Subject.
- **Material was out of the freshness window** (`kind = "out_of_window"`): the only material located was too stale to contribute.
- **Other** (`kind = "other"`): document an explanation; do not reuse this as a catch-all that hides the real reason.

In all `insufficient` cases the Result must:

- Carry an `insufficientDataReason` with `kind` and `explanation`.
- Carry a non-empty `suggestedNextSources` array. Suggestions must be **concrete source types** (e.g. "Glassdoor reviews for Acme Corp", not "search harder").
- Confidence rating must be `low` or `unknown` only.
- Carry no themes, signals, or inference.

The schema enforces these by *omitting* themes/signals/inference from the `insufficient` arm of the discriminated union and forcing `.strict()` on it — they are not just rejected at validation, they are unrepresentable.

### `unsupported_category`
The request was for a Decision Category KURO does not evaluate. This is a **scope refusal**, never an evidence assessment. KURO did not attempt to gather, interpret, or weigh community material — there is no "low-confidence finding hidden inside a scope refusal." Required fields:

- `requestedCategory` — the unsupported category string the caller asked for.
- `supportedCategories` — the exact MVP category list.
- `refusalMessage` — plain-language explanation.

`unsupported_category` may not carry signals, themes, or inference. The schema's strict object on this arm rejects any such key.

## 4. Allowed and forbidden output per status

| Field | `sufficient` | `partial` | `insufficient` | `unsupported_category` |
|---|:---:|:---:|:---:|:---:|
| summary | ✓ | ✓ (narrow) | ✓ (descriptive only) | ✗ |
| sourceDocuments / attributions | required | required | optional | forbidden |
| sourceSummary | required | required | optional | forbidden |
| sourceCoverage | — | — | optional, recommended when sources were found | forbidden |
| evidence / signals / themes | required (non-empty) | required (non-empty) | forbidden | forbidden |
| inference | required | required (narrow) | forbidden | forbidden |
| evidenceGaps | — | **required (non-empty)** | — | — |
| insufficientDataReason | — | — | **required** | — |
| suggestedNextSources | — | optional (if relevant) | **required (non-empty)** | — |
| confidence.rating | `low`/`medium`/`high` | `low`/`medium` | `low`/`unknown` | — |
| confidence reasons | required | required | required | — |
| limitations | optional | optional | optional | — |
| finalKuro | required | required | required | — |
| requestedCategory | — | — | — | **required** |
| supportedCategories | — | — | — | **required** |
| refusalMessage | — | — | — | **required** |

`—` = the field does not appear in this arm of the schema.

## 5. Forbidden behaviors (always)

- Fabricating themes, signals, summaries, or confidence to fill missing evidence.
- "Based on common patterns…" or generic likelihood language unanchored to Evidence.
- Appending an "insufficient data" disclaimer to a normal-shaped Result; the **status itself** carries that signal.
- Returning `insufficient` for an unsupported category (must be `unsupported_category`).
- Returning `unsupported_category` for thin evidence in a supported category (must be `insufficient` or `partial`).
- Collapsing conflicting evidence into a single confident conclusion (see §7).
- Emitting `high` confidence on any `partial` or `insufficient` Result.
- Emitting a `signal` without at least one supporting `Evidence` (`No Evidence, no Signal`).

## 6. Discriminated-union representation

The schema models `KuroResult` as a discriminated union on `dataSufficiency` (`packages/schemas/src/result.ts`). Each variant is a distinct object schema. The `insufficient` and `unsupported_category` variants are marked `.strict()`, which rejects any unknown keys — including the evidence-shaped keys (`themes`, `signals`, `evidence`, `inference`) — that would silently slip past a non-strict object.

This is preferred over a flat object with optional fields because:

1. **Illegal states are unrepresentable, not merely invalid.** TypeScript narrowing on `dataSufficiency` exposes only the fields legal for that status; a caller cannot write `r.themes` on a known-insufficient result and get past `tsc`.
2. **The schema itself is the contract**; reviewers do not have to mentally cross-reference a free-form rules table against a flat-shaped object to know which combinations are legal.
3. **Forbidden combinations need fewer hand-written `superRefine` checks**, lowering the risk that a future field addition silently relaxes a forbidden combination.

The trade-off is that variants do not share a structural type — callers must `switch` on `dataSufficiency` to access status-specific fields. This is the right cost: it forces callers to handle each status explicitly, which is exactly the posture the issue requires.

## 7. Conflicting evidence is NOT insufficient

Conflicting Evidence is a successful KURO outcome, not a refusal. KURO must represent disagreement explicitly:

- Use `Theme.sentiment = "mixed"` where Signals carry genuine internal tension.
- Use `Inference.disagreements` to surface divergent themes.
- Keep both sides of the disagreement traceable to their Evidence — do not flatten "some say X, some say Y" into a confident composite.

A Result with broad, conflicting Evidence may be `sufficient` (with explicit disagreement) or `partial` (if breadth is also narrow). It is **never** `insufficient` solely because Signals disagree. See [GLOSSARY.md §Theme](./GLOSSARY.md) and the `mixed` sentiment guidance on Signals.

## 8. Wording — what KURO may and may not say

KURO's posture extends to its prose. Acceptable wording stays close to source-backed observation; unacceptable wording leaks generic judgment into the gap left by missing Evidence.

### Acceptable

- "KURO could not support a meaningful inference from the available evidence."
- "The available evidence only supports a narrow observation about compensation sentiment."
- "No supported conclusion is returned because the retrieved sources did not contain usable first-hand feedback."
- "Reviewers within the available material disagree on noise levels by unit orientation."

### Unacceptable

- "This company is probably good."
- "This landlord seems risky."
- "There is not enough data, but based on common patterns…"
- "Likely negative."
- Any generic likelihood statement not anchored to retrieved Evidence.

`summary` and `finalKuro` on `partial` and `insufficient` Results must obey these constraints. The schema cannot enforce wording, but every result-rendering surface and prompt referenced by KURO must reference this section.

## 9. Worked examples

### Employment Intelligence

#### No sources found → `insufficient`
A query for "NoNames Industries" turns up zero Source Documents.

- `dataSufficiency`: `insufficient`
- `insufficientDataReason.kind`: `no_sources_found`
- `sourceDocuments`: `[]`
- `suggestedNextSources`: includes Glassdoor reviews, LinkedIn employee posts, and a relevant industry-association forum.
- `confidence.rating`: `unknown`

Fixture: [`examples/employment-no-sources.json`](../packages/schemas/examples/employment-no-sources.json).

#### Partial evidence only (compensation only) → `partial`
A query for "ThinCorp" returns two Blind posts about pay, nothing about management, culture, or work-life balance.

- `dataSufficiency`: `partial`
- One Theme (Compensation) supported by two Signals; nothing else.
- `evidenceGaps` lists Management, Culture, Work-Life Balance, Career growth.
- `confidence.rating`: `low`.
- `summary` describes only what Evidence supports; `finalKuro` may not characterize axes for which Evidence is missing.

Fixture: [`examples/employment-partial.json`](../packages/schemas/examples/employment-partial.json).

#### Sources found but no usable Evidence → `insufficient`
A query for "Obscure Holdings LLC" returns two documents — a 2022 careers blog and a directory listing — neither of which carries first-hand experience.

- `dataSufficiency`: `insufficient`
- `insufficientDataReason.kind`: `no_usable_evidence`
- `sourceCoverage` explains *why* each document was unusable.
- `suggestedNextSources` recommends specific source types.

Fixture: [`examples/insufficient-data.json`](../packages/schemas/examples/insufficient-data.json).

### Rental Intelligence

#### Sources found but no usable Evidence → `insufficient`
A query for "45 Pine St" returns a landlord listing, an aggregator ad, and an unrelated neighborhood thread about a nearby coffee shop.

- `dataSufficiency`: `insufficient`
- `insufficientDataReason.kind`: `no_usable_evidence`
- `sourceCoverage` flags two `promotional` documents and one `not_about_subject` document.
- `suggestedNextSources` names verified tenant reviews and the local subreddit.

Fixture: [`examples/rental-unusable-sources.json`](../packages/schemas/examples/rental-unusable-sources.json).

#### Conflicting but sufficient Evidence → `sufficient`
A query for "123 Main St, Apt 4B" returns three sources from three platforms, with consistent negatives on maintenance, a positive on location, and divergent reports on noise depending on unit orientation.

- `dataSufficiency`: `sufficient`
- `Theme.sentiment = "mixed"` on the Noise theme.
- `Inference.disagreements` calls out the noise divergence by unit.
- The composite is not flattened — neither "the building is noisy" nor "the building is quiet" is asserted.

Fixture: [`examples/rental-123-main.json`](../packages/schemas/examples/rental-123-main.json).

### Unsupported category refusal

A query for `banking` is rejected on scope grounds.

- `dataSufficiency`: `unsupported_category`
- `requestedCategory`: `banking`
- `supportedCategories`: `["employment_intelligence", "rental_intelligence"]`
- `refusalMessage`: explains scope and notes that this is a scope refusal, not an evidence assessment.
- No themes, signals, evidence, or inference — these keys are unrepresentable on this arm.

Fixture: [`examples/result.invalid-category.json`](../packages/schemas/examples/result.invalid-category.json).

## 10. Validation rules and their negative fixtures

Every rule below has a negative fixture in [`packages/schemas/examples/validate.ts`](../packages/schemas/examples/validate.ts) that demonstrates rejection.

- `insufficient` with non-empty `themes` → rejected (strict object).
- `insufficient` with any `signals` → rejected (strict object).
- `insufficient` with any `evidence` → rejected by absence from the arm + strict.
- `insufficient` with `high` or `medium` confidence → rejected.
- `insufficient` without `suggestedNextSources` (or with empty array) → rejected.
- `insufficient` without `insufficientDataReason` → rejected.
- `insufficient` with `kind: "no_sources_found"` and non-empty `sourceDocuments` → rejected.
- `insufficient` with `sourceCoverage` referencing an unknown sourceDocumentId → rejected.
- `partial` without `evidenceGaps` (or with empty array) → rejected.
- `partial` with `high` or `unknown` confidence → rejected.
- `partial` with empty `inference.limitations` → rejected.
- `sufficient` with empty themes / sourceDocuments → rejected.
- `sufficient` with `confidence.rating = "unknown"` → rejected.
- `sufficient` with `confidence.rating = "high"` and fewer than 3 themes at `medium+` → rejected (breadth cap).
- Every `signal` without at least one `evidenceId` → rejected at the Signal schema (`No Evidence, no Signal`).
- `unsupported_category` with a *supported* `requestedCategory` → rejected.
- `unsupported_category` whose `supportedCategories` does not match the MVP set → rejected.
- `unsupported_category` carrying themes / signals / evidence → rejected (strict).

## 11. Non-goals

This document does not specify:

- How sources are retrieved or scraped.
- How confidence scores are computed (see [CONFIDENCE.md](./CONFIDENCE.md)).
- Any Decision Category beyond MVP (`employment_intelligence`, `rental_intelligence`).
- End-user UI/UX copy. Surfaces must honor the wording guidance in §8 but choose their own concrete phrasing per surface.
- Pipeline-side handling of retries or backoff when sources are temporarily unavailable; that is a fetch-layer concern.
