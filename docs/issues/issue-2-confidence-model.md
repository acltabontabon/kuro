# Issue #2 — Refined: Define the KURO Confidence Model (MVP)

> Replaces the current one-paragraph issue body. Labels: `ws1-product`, `design`, `mvp`.

## Opinion up front

The current issue is too thin and conflates two questions: **(a)** what does confidence *mean* in KURO, and **(b)** how is it *computed and represented*. These should be answered in that order, and the model document should land before any computation work.

I do **not** recommend splitting this into multiple issues yet. The semantic definition, the representation, and the contributing factors are tightly coupled — splitting them risks each ticket being decided in isolation and producing an incoherent whole. One ticket, one document, one PR.

A follow-up ticket *should* be opened later for the actual computation/aggregation logic once the model is locked. That is implementation, not design.

Note: a substantial confidence shape already exists in [`packages/schemas/src/confidence.ts`](packages/schemas/src/confidence.ts) (ratings + optional `supportScore` + structured `inputs` at Signal, Theme, Result). This ticket's job is to ratify, document, and tighten that model — not to reinvent it. Several recommendations below propose schema changes; flagged inline.

---

## Issue body (rewritten)

### Summary

Define and document KURO's confidence model: what it means, what it does not mean, how it is represented in the user/API-facing Result, what contributes to it, and how it behaves under uncertainty, disagreement, weak evidence, and insufficient data.

The output of this ticket is a canonical confidence section in [`docs/GLOSSARY.md`](docs/GLOSSARY.md) (expanding the existing one) plus a short companion document (`docs/CONFIDENCE.md`) that pins the MVP rules concretely enough for implementers and reviewers to apply without further interpretation. Any schema deltas land in [`@kuro/schemas`](packages/schemas) in the same PR.

### What confidence means in KURO

**Confidence is strength-of-support, not truth-probability.** It expresses how well-grounded an interpretation is in the evidence KURO actually read. A `high` confidence Theme means "the Signals supporting this Theme are numerous, diverse, recent, and consistent" — it does **not** mean "this claim about the Subject is objectively true."

This framing is non-negotiable and follows directly from KURO's posture (Glossary §2): KURO presents informed inferences, not facts.

### What confidence does NOT mean

- It is **not** a probability that the claim is true in the world.
- It is **not** a recommendation strength (KURO does not recommend).
- It is **not** a sentiment score. A negative Theme can be high-confidence; a positive Theme can be low-confidence.
- It is **not** an "is there enough data" flag. That is the separate `outcome: insufficient_data` branch on the Result.
- It is **not** a quality score for the Subject.

### Representation (MVP)

Use **qualitative bands** as the primary, user-facing representation. Allowed bands differ by level:

| Band | Signal | Theme | Result | Meaning |
|---|---|---|---|---|
| `high` | ✓ | ✓ | ✓ | Multiple diverse, recent, internally-consistent Signals. The interpretation rests on broad support. |
| `medium` | ✓ | ✓ | ✓ | Some support, but limited on at least one axis (narrow source set, partial agreement, mixed freshness). |
| `low` | ✓ | ✓ | ✓ | Weak support — few sources, low diversity, stale, contradictory, or thinly interpreted. KURO is showing the pattern; the user should treat it as tentative. |
| `unknown` | ✗ | ✗ | ✓ | **Result-level only.** Used when `outcome: insufficient_data` and there is no usable evidence to score against. |

**Why `unknown` is Result-only.** A Signal exists only because Evidence supports it; if KURO cannot rate the interpretation, the Signal should not be emitted. A Theme exists only because ≥1 Signal supports it; if KURO cannot rate the Theme, it should not be emitted. Allowing `unknown` at Signal/Theme creates exactly the lazy escape hatch this ticket is meant to avoid. **Enforce in the schema with separate rating enums per level** (see Schema implications).

**On numeric scores.** The schema already permits an optional `supportScore` (0–1) at every level. Keep it **internal-only** for MVP — do not surface it in the user-facing UI or the public API, and do not require producers to emit it. Reasons:

1. A 0.62 is not more honest than `medium`; it is more *precise-looking* than honest. Fake precision is the failure mode we are explicitly avoiding.
2. We have no calibration data yet. Without calibration, any number is theatre.
3. Reserving the field now lets us add numeric output later without a breaking schema change.

### Levels at which confidence is attached

Confidence is attached at **three levels**, matching the existing schema:

1. **Signal confidence** — how confidently the interpretation matches its Evidence.
2. **Theme confidence** — how well-supported the Theme is by its Signals.
3. **Result confidence** — how well-supported the overall picture is.

**Composition rule:** Result confidence is **not** a simple max or mean of Theme confidences. A Result composed of one high-confidence Theme is not a high-confidence Result, because *breadth* of topic coverage is a separate input. This is already captured in the glossary; lift it into the rules.

### Contributing factors (drivers)

These are the inputs documented and enforced. Each driver is described qualitatively for MVP; no weighted formula yet.

**Signal-level drivers**
- **Clarity of excerpt.** Is the Evidence text unambiguous?
- **Language ambiguity.** Hedging, sarcasm, conditional phrasing reduce confidence.
- **Directness of support.** Does the Evidence directly state the claim, or is the claim inferred from context?

**Theme-level drivers**
- **Source count.** How many Signals support the Theme.
- **Source diversity.** How many *distinct* Source Documents and platforms. Five Signals from one Reddit thread is not five sources.
- **Source freshness.** How recent the underlying Source Documents are. **MVP fallback rule:** use a rolling **24-month window**. Sources within the window contribute normally; sources older than 24 months contribute at reduced weight and must be called out in `reasons` if they dominate the Theme. Subject-kind-specific windows are a follow-up ticket; this default unblocks implementation now.
- **Signal consistency.** Agreement among the Signals in the Theme on **claim content** (what is being said about the Subject), **not** on sentiment polarity. A Theme with `sentiment: "mixed"` where each side internally agrees on its claims is *consistent* for confidence purposes. Consistency only drops when Signals disagree about **facts or interpretations** (e.g., one says "open-plan office", another says "private offices"). See "Disagreement among Signals" below.

**Result-level drivers**
- **Theme support aggregate.** A roll-up of Theme confidences.
- **Topic breadth.** Coverage across distinct topic Themes. **MVP rule (concrete cap):**
  - Result `high` requires **≥ 3 Themes** with rating ≥ `medium`, **and** Theme support aggregate ≥ `medium`.
  - With **1–2 Themes** total, the Result is capped at `medium` regardless of individual Theme confidence. A single high-confidence Theme is not a high-confidence Result.
  - With **0 supporting Themes**, the Result is `low` or `unknown` and `outcome` must be `insufficient_data`.

  These thresholds are deliberately blunt for MVP. "What topics one would expect for the Subject kind" is a follow-up; until then, breadth = Theme count at medium+ confidence.

These match `BaseConfidenceInputs` / `SignalConfidence` / `ThemeConfidence` / `ResultConfidence` in [`confidence.ts`](packages/schemas/src/confidence.ts) and should remain authoritative.

### How confidence reacts to specific situations

**Uncertainty in language.** Hedged Evidence ("might be," "sometimes") lowers Signal confidence; it does not lower Theme confidence directly — a Theme of consistently-hedged Signals can still be a clearly-supported pattern of hedged opinion.

**Disagreement among Signals.** Mixed sentiment within a Theme is a *feature*, not a confidence problem. It is represented by `sentiment: "mixed"` at the Theme level. Theme confidence should remain `medium` or `high` if the Signals on both sides are themselves well-supported. Lower confidence **only** when the disagreement is on the *facts* being reported, not on the *sentiment* about them.

**Weak evidence.** Few sources, single platform, stale dates, or thin interpretation → `low`. The reason must appear in `reasons` (see below).

**Insufficient data.** Distinct from `low`. When KURO cannot extract enough usable Evidence to responsibly infer anything, the **Result outcome** flips to `insufficient_data` and confidence is `low` or `unknown` (already enforced by [`result.ts` superRefine](packages/schemas/src/result.ts)). Insufficient data is a property of the Result, not a confidence band — keep it that way.

### Drivers and reasons in the output

Every confidence object must be **explainable**. The schema carries structured `inputs` (e.g. `sourceCount`, `sourceDiversity`). That is good for machines, weak for humans.

**Add a `reasons` field** to every level of confidence:

```
reasons: Array<{
  driver: "sourceCount" | "sourceDiversity" | "sourceFreshness"
        | "signalConsistency" | "clarity" | "languageAmbiguity"
        | "directnessOfSupport" | "themeSupportAggregate" | "topicBreadth";
  effect: "raises" | "lowers" | "neutral";
  note: string; // short human-readable, e.g. "only 2 sources, both from r/jobs"
}>
```

**`reasons` is required at every level, including `high`.** Minimum length 1, no conditional rule based on rating. Rationale:

- A `high` rating without justification is just as opaque as a `low` one.
- A flat rule is trivial to enforce in the schema; a rating-conditional rule invites "mark `high` to skip explanation."
- Reviewers, auditors, and the UI need a uniform shape.

This is a schema delta; treat it as part of this ticket's PR.

### What the public API exposes vs internal

**Public/user-facing API response** — strictly:
- `rating` (the band).
- `reasons` (≥1, with human-readable `note`).

That is the entire public confidence contract. Nothing else surfaces by default.

**Internal / debug-only** (available behind a debug flag, never on the default response):
- `inputs` — the structured numeric drivers (`sourceCount`, `sourceDiversity`, etc.). Useful for analytics and review tools; misleading on a product surface because the floats look calibrated and are not.
- `supportScore` — the optional internal numeric rollup.

The public API ticket (separate workstream) should pin this split with a response-shape test that proves `inputs` and `supportScore` are absent from the default payload.

UI guidance for the first surface:
- Show the band as a label, not a number, not a progress bar.
- Make `reasons` reachable on hover/expand. Never hide a `low` or `unknown` rating.
- Do not color-code confidence using the same palette as sentiment.

---

## Acceptance criteria

- [ ] `docs/GLOSSARY.md` Confidence section is updated to reflect this model (already mostly there; verify and tighten language on disagreement vs low-confidence).
- [ ] A new `docs/CONFIDENCE.md` is added containing: the four bands with definitions, the driver list per level, the situation-handling rules (uncertainty / disagreement / weak / insufficient), and the API/UI exposure rules.
- [ ] `packages/schemas/src/confidence.ts` includes a **required** `reasons` field (min length 1) on `SignalConfidence`, `ThemeConfidence`, `ResultConfidence` — no conditional rule on rating.
- [ ] `SignalConfidence.rating` and `ThemeConfidence.rating` are restricted to `low | medium | high` (no `unknown`). Only `ResultConfidence.rating` admits `unknown`.
- [ ] `supportScore` and `inputs` are documented as internal/debug-only and are **not** part of the public API response shape.
- [ ] `outcome: insufficient_data` continues to require Result `rating` ∈ {`low`, `unknown`} (already enforced; add a test for the `unknown` path).
- [ ] A Result with fewer than 3 Themes at rating ≥ `medium` cannot have Result `rating: "high"` (schema-enforced or test-enforced).
- [ ] At least three worked examples (below) are checked into `packages/schemas/examples/` and validated by the existing example runner.
- [ ] No weighted formula is introduced. Computation is out of scope; this ticket defines the model only.
- [ ] README of `@kuro/schemas` cross-links to `docs/CONFIDENCE.md`.

---

## Recommended MVP model (concise restatement)

- **User-facing bands:** `low | medium | high` at Signal/Theme; `low | medium | high | unknown` at Result only.
- **Public API** exposes `rating` + `reasons` only. `inputs` and `supportScore` are internal/debug.
- **`reasons` required at every level**, ≥1 entry, including `high`. Flat rule.
- **Per-level confidence**: Signal, Theme, Result — composed bottom-up but **not** by simple aggregation (Result also requires breadth: ≥3 Themes at medium+ for `high`).
- **Drivers** are the existing structured `inputs`. No new factors for MVP.
- **Freshness fallback:** rolling 24-month window.
- **`signalConsistency`** measures claim/factual agreement, not sentiment polarity. Mixed sentiment does not lower confidence.
- **Insufficient data** stays a Result-level outcome, not a confidence band.
- **Confidence is independent** of sentiment, recommendation strength, and insufficiency.

---

## Edge cases

1. **Single source, multiple Signals.** Source count looks healthy; diversity is 1. Theme confidence ≤ `medium`. `reasons` must call out the single-source caveat.
2. **Fresh sources, all agree, but only two of them.** `medium` at best. Two sources is not a pattern.
3. **Many sources, all from one platform.** `medium` ceiling unless the Subject is genuinely platform-bound (e.g. an open-source project's GitHub issues).
4. **Sentiment is mixed but each side is well-supported.** Theme confidence stays `high`; `sentiment` is `mixed`. The two are orthogonal.
5. **Factual disagreement** ("the office is in Berlin" vs "the office is in Munich"). Theme confidence drops to `low`; `reasons` notes the contradiction; the Inference's `mayNotSuggest` should reflect the unresolved fact.
6. **One Theme is `high`, the other six are `low`.** Result confidence is `low` or `medium`, never `high` — breadth is uneven.
7. **No usable Evidence at all.** `outcome: insufficient_data`, Result confidence `unknown`, every Theme and Signal array empty. Already enforced in [`result.ts`](packages/schemas/src/result.ts).
8. **Stale but consistent corpus** (e.g. all reviews from 4+ years ago). At most `medium`; `reasons` must include a freshness note. The Inference's `limitations` must mirror this.
9. **One very long Source Document yields many Signals.** Treat as one source for diversity; flag in `reasons` if it dominates the Theme.
10. **Sarcasm / heavy hedging.** Signal-level clarity drops. Cascades upward only if such Signals dominate the Theme.

---

## Examples of confidence outputs

### Example A — strong Theme

```json
{
  "level": "theme",
  "rating": "high",
  "inputs": {
    "sourceCount": 12,
    "sourceDiversity": 0.8,
    "sourceFreshness": 0.9,
    "signalConsistency": 0.85
  },
  "reasons": [
    { "driver": "sourceCount",      "effect": "raises", "note": "12 supporting Signals across 9 sources" },
    { "driver": "sourceDiversity",  "effect": "raises", "note": "4 distinct platforms" },
    { "driver": "sourceFreshness",  "effect": "raises", "note": "median age 4 months" }
  ]
}
```

### Example B — weak Theme (single platform, recent, agreeing)

```json
{
  "level": "theme",
  "rating": "low",
  "inputs": {
    "sourceCount": 3,
    "sourceDiversity": 0.2,
    "sourceFreshness": 0.9,
    "signalConsistency": 0.9
  },
  "reasons": [
    { "driver": "sourceCount",     "effect": "lowers", "note": "only 3 Signals" },
    { "driver": "sourceDiversity", "effect": "lowers", "note": "all 3 from r/jobs, one author wrote 2 of them" }
  ]
}
```

### Example C — insufficient data

```json
{
  "outcome": "insufficient_data",
  "confidence": {
    "level": "result",
    "rating": "unknown",
    "inputs": {},
    "reasons": [
      { "driver": "sourceCount", "effect": "lowers", "note": "0 usable Source Documents after filtering" }
    ]
  },
  "inference": {
    "limitations": [
      "No usable community material was found for this Subject within the freshness window."
    ]
  }
}
```

### Example D — mixed sentiment, high confidence

Note `signalConsistency` is **high** here (0.85) because the consistency driver measures claim/factual agreement, not sentiment polarity. The Signals on each side internally agree on what they're claiming.

```json
{
  "sentiment": "mixed",
  "confidence": {
    "level": "theme",
    "rating": "high",
    "inputs": { "sourceCount": 18, "sourceDiversity": 0.7, "signalConsistency": 0.85 },
    "reasons": [
      { "driver": "sourceCount",       "effect": "raises", "note": "18 Signals across 11 sources" },
      { "driver": "signalConsistency", "effect": "raises", "note": "each side internally agrees on its claims; the split is sentiment, not facts" }
    ]
  }
}
```

---

## Schema implications for `@kuro/schemas`

Concrete deltas to land with this ticket:

1. **Split the rating enum by level.** In `primitives.ts`, replace the single `ConfidenceRating` with:
   - `SubResultConfidenceRating = z.enum(["low", "medium", "high"])` — used by `SignalConfidence` and `ThemeConfidence`.
   - `ResultConfidenceRating = z.enum(["low", "medium", "high", "unknown"])` — used by `ResultConfidence`.

   `unknown` becomes unrepresentable at Signal and Theme.
2. **Add a required `reasons` array** on all three confidence variants, `min(1)`, with shape `{ driver, effect, note }`. No conditional rule on rating — flat min-1 everywhere.
3. **Define a shared `ConfidenceDriver` enum** in `primitives.ts` covering: `sourceCount`, `sourceDiversity`, `sourceFreshness`, `signalConsistency`, `clarity`, `languageAmbiguity`, `directnessOfSupport`, `themeSupportAggregate`, `topicBreadth`. Type-safe across levels.
4. **`effect` enum:** `z.enum(["raises", "lowers", "neutral"])`.
5. **Mark `supportScore` and `inputs` as internal.** Doc-comment: `@internal — not part of the public API response shape`. Schema keeps them, public-API serializer omits them.
6. **Keep the existing `inputs` field shape.** Do not add new driver fields for MVP.
7. **Update `result.ts` superRefine:**
   - Existing `outcome === "insufficient_data"` → `rating ∈ {low, unknown}` rule is kept (now safe to mention `unknown` because `ResultConfidence` is the only place it lives).
   - **Add:** if Result `rating === "high"`, require `themes.filter(t => t.confidence.rating !== "low").length >= 3`. Enforces the breadth cap.
   - Add tests for the `unknown` path and the breadth cap.
8. **No weighted formula in the schema.** The schema describes the *shape* of confidence, not its computation. Computation is the follow-up ticket.

---

## Out of scope (explicitly)

- The actual algorithm that turns Signals into Theme confidence. Open a separate `ws2` implementation ticket once this model is merged.
- Calibration data and any move toward exposing numeric scores publicly.
- Per-Subject-kind tuning of freshness windows (note it; do not solve it here).
- UI design for the confidence affordance beyond the exposure rules above.
