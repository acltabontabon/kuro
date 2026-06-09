# KURO Confidence Model (MVP)

This document pins the MVP rules for KURO's confidence model concretely enough for implementers and reviewers to apply without further interpretation. The definitions live in [GLOSSARY.md §Confidence](./GLOSSARY.md#confidence); this file is the rulebook.

Confidence applies within whatever [Decision Category](./DECISION_CATEGORIES.md) the Result declares; category bounds scope and interpretation, but does not change how confidence is computed or what it means.

KURO confidence describes **strength of support**, not truth-probability. A `high` confidence Theme means "the Signals supporting this Theme are numerous, diverse, recent, and consistent." It does **not** mean "this claim about the Subject is objectively true."

## 1. What confidence does NOT mean

- It is **not** a probability that the claim is true in the world.
- It is **not** a recommendation strength. KURO does not recommend.
- It is **not** a sentiment score. A negative Theme can be high-confidence; a positive Theme can be low-confidence.
- It is **not** an "is there enough data" flag. That is the separate `dataSufficiency` discriminator on the Result (`sufficient` / `partial` / `insufficient` / `unsupported_category` — see [INSUFFICIENT_DATA.md](./INSUFFICIENT_DATA.md)).
- It is **not** a quality score for the Subject.

## 2. Bands

Qualitative bands are the primary, user-facing representation.

| Band | Signal | Theme | Result | Meaning |
|---|:---:|:---:|:---:|---|
| `high` | ✓ | ✓ | ✓ | Multiple diverse, recent, internally-consistent Signals. Broad support. |
| `medium` | ✓ | ✓ | ✓ | Some support, but limited on at least one axis (narrow source set, partial agreement, mixed freshness). |
| `low` | ✓ | ✓ | ✓ | Weak support — few sources, low diversity, stale, contradictory, or thinly interpreted. |
| `unknown` | ✗ | ✗ | ✓ | **Result-level only.** Used when `dataSufficiency: insufficient` and there is no usable evidence to score against. |

**Caps by `dataSufficiency`.**

| dataSufficiency | Allowed Result confidence ratings |
|---|---|
| `sufficient` | `low`, `medium`, `high` (subject to the breadth cap below) |
| `partial` | `low`, `medium` only — `high` and `unknown` forbidden |
| `insufficient` | `low`, `unknown` only — `medium` and `high` forbidden |
| `unsupported_category` | n/a — confidence is not carried on a scope refusal |

`unknown` is **unrepresentable** at Signal and Theme by construction: a Signal exists only because Evidence supports it, and a Theme exists only because at least one Signal supports it. The schema enforces this via separate rating enums per level (`SubResultConfidenceRating` for Signal and Theme, `ResultConfidenceRating` for Result).

## 3. Levels and composition

Confidence is attached at three levels:

1. **Signal confidence** — how confidently the interpretation matches its Evidence.
2. **Theme confidence** — how well-supported the Theme is by its Signals.
3. **Result confidence** — how well-supported the overall picture is.

**Composition rule.** Result confidence is **not** a simple max or mean of Theme confidences. A Result composed of one high-confidence Theme is not a high-confidence Result, because *breadth of topic coverage* is a separate input.

## 4. Drivers per level

These are the inputs that move a rating. Each driver is described qualitatively for MVP; no weighted formula is defined.

**Signal-level drivers**

- **Clarity of excerpt.** Is the Evidence text unambiguous?
- **Language ambiguity.** Hedging, sarcasm, conditional phrasing reduce confidence.
- **Directness of support.** Does the Evidence directly state the claim, or is the claim inferred from context?

Evidence carries its own quality hints — `sourceTrust`, duplicate markers, reviewer notes, and the synthesized-vs-verbatim distinction — that feed these Signal-level drivers. The hints are the source of truth for *what* the support is; the drivers above are the source of truth for *how strong* it is. See [EVIDENCE.md](./EVIDENCE.md) for the hint definitions and the rule that synthesized Evidence must justify the lack of a verbatim anchor.

**Theme-level drivers**

- **Source count.** How many Signals support the Theme.
- **Source diversity.** How many *distinct* Source Documents and platforms. Five Signals from one Reddit thread is not five sources.
- **Source freshness.** How recent the underlying Source Documents are.
- **Signal consistency.** Agreement among the Signals in the Theme on **claim content** (what is being said about the Subject), **not** on sentiment polarity. See §6 *Disagreement*.

**Result-level drivers**

- **Theme support aggregate.** A roll-up of Theme confidences.
- **Topic breadth.** Coverage across distinct topic Themes.

The schema field name for a driver matches the values of the `ConfidenceDriver` enum: `sourceCount`, `sourceDiversity`, `sourceFreshness`, `signalConsistency`, `clarity`, `languageAmbiguity`, `directnessOfSupport`, `themeSupportAggregate`, `topicBreadth`.

## 5. MVP concrete rules

### Freshness window

Sources within a rolling **24-month window** contribute normally. Sources older than 24 months contribute at reduced weight and must be called out in `reasons` if they dominate the Theme. Per-Subject-kind freshness windows are a follow-up; this default unblocks implementation now.

### Result rating caps (breadth)

- Result `high` requires **≥ 3 Themes** with confidence rating ≥ `medium`, **and** Theme support aggregate ≥ `medium`.
- With **1–2 Themes** total, the Result is capped at `medium` regardless of individual Theme confidence. A single high-confidence Theme is not a high-confidence Result.
- With **0 supporting Themes**, the Result is `low` or `unknown`, and `dataSufficiency` must be `insufficient` (or `partial` if there is some narrow signal worth surfacing without a Theme).

The ≥ 3 Themes at medium+ rule is schema-enforced in [`packages/schemas/src/result.ts`](../packages/schemas/src/result.ts) `superRefine`. Future work may refine "what topics one would expect for the Subject kind"; until then, breadth = Theme count at medium+ confidence.

### Numeric scores

The schema permits an optional `supportScore` (0–1) at every level. Keep it **internal-only** for MVP. Do not surface it in the user-facing UI or the public API; do not require producers to emit it. Reasons:

1. A 0.62 is not more honest than `medium`; it is more *precise-looking* than honest. Fake precision is the failure mode we are avoiding.
2. We have no calibration data yet. Without calibration, any number is theatre.
3. Reserving the field now lets us add numeric output later without a breaking schema change.

## 6. Situation handling

**Uncertainty in language.** Hedged Evidence ("might be," "sometimes") lowers Signal confidence. It does **not** lower Theme confidence directly — a Theme of consistently-hedged Signals can still be a clearly-supported pattern of hedged opinion.

**Disagreement among Signals.** Mixed sentiment within a Theme is a *feature*, not a confidence problem. It is represented by `sentiment: "mixed"` at the Theme level. Theme confidence remains `medium` or `high` if the Signals on both sides are themselves well-supported. Lower confidence **only** when the disagreement is on the *facts* being reported, not on the *sentiment* about them.

- Mixed sentiment, each side internally consistent on claims → high confidence is fine.
- Two Signals contradict each other on a fact ("office is in Berlin" vs "office is in Munich") → confidence drops; `reasons` notes the contradiction.

**Weak evidence.** Few sources, single platform, stale dates, or thin interpretation → `low`. The reason must appear in `reasons`.

**Insufficient data.** Distinct from `low`. When KURO cannot extract enough usable Evidence to responsibly infer anything, the **Result's `dataSufficiency`** is `insufficient` and confidence is `low` or `unknown` (enforced by `result.ts` `superRefine`). Insufficient data is a status of the Result, not a confidence band; see [INSUFFICIENT_DATA.md](./INSUFFICIENT_DATA.md).

## 7. The `reasons` contract

Every confidence object — Signal, Theme, and Result — carries a required `reasons` array, minimum length 1, including when `rating: "high"`. Each entry has the shape:

```ts
{
  driver: ConfidenceDriver,           // see §4 driver list
  effect: "raises" | "lowers" | "neutral",
  note: string                        // short human-readable
}
```

The rule is flat — no conditional on rating. Rationale:

- A `high` rating without justification is just as opaque as a `low` one.
- A flat rule is trivial to enforce; a rating-conditional rule invites "mark `high` to skip explanation."
- Reviewers, auditors, and the UI need a uniform shape.

## 8. Public API vs internal

**Public, user-facing confidence shape** — strictly:

- `rating` (the band)
- `reasons` (≥ 1, with human-readable `note`)

That is the entire public confidence contract.

**Internal / debug-only** (available behind a debug flag, never on the default response):

- `inputs` — the structured numeric drivers. Useful for analytics and review tools; misleading on a product surface because the floats look calibrated and are not.
- `supportScore` — the optional internal numeric rollup.

The public-API workstream owns the response-shape test that proves `inputs` and `supportScore` are absent from the default payload.

## 9. UI guidance

- Show the band as a **label**, not a number, not a progress bar.
- Make `reasons` reachable on hover or expand. Never hide a `low` or `unknown` rating.
- Do **not** color-code confidence using the same palette as sentiment — keep the two dimensions visually distinct.

## 10. Out of scope (MVP)

- The actual algorithm that turns Signals into Theme confidence. That is a separate `ws2` implementation ticket once the model is merged.
- Calibration data and any move toward exposing numeric scores publicly.
- Per-Subject-kind tuning of freshness windows.
- UI design for the confidence affordance beyond the exposure rules above.

## 11. Cross-links

- Source Attribution (the separate provenance layer that records *where* a Source Document came from): [SOURCE_ATTRIBUTION.md](./SOURCE_ATTRIBUTION.md). Confidence describes strength of support across read evidence; attribution describes the surface that evidence was lifted from. **Trust tier is a reliability hint, never a truth probability**, and it must not be conflated with a confidence band.
