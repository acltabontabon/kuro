# KURO Evidence Model (MVP)

This document is the rulebook for KURO's Evidence layer. The conceptual definition lives in [GLOSSARY.md §Evidence](./GLOSSARY.md#evidence); this file pins the traceability rules, locator kinds, quality hints, and edge-case handling that the schema and the extraction pipeline must agree on.

## 1. Definition

> **Evidence** is a traceable, addressable extract from a single Source Document that *could* be used to support one or more Signals. It is raw or minimally-normalized material plus the provenance needed to locate it again in the original source. Evidence does **not** carry interpretation, stance, sentiment, or conclusions.

Evidence is the only layer in the KURO pipeline that does not interpret. Everything from the Signal layer up is opinion about Evidence; Evidence itself is the citation.

## 2. What Evidence is not

| Concept | Carries | Interpretive? |
|---|---|---|
| Source Document | Full original input | No |
| **Evidence** | **Addressable extract + provenance** | **No** |
| Signal | A claim or opinion derived from one or more Evidence records | Yes |
| Theme | A grouping of related Signals by topic | Yes (structural) |
| KURO Inference | Cross-Theme pattern, consensus, disagreement, limitations | Yes |
| KURO Result | The user-facing assembly of the above | Yes (presentational) |

If a candidate field expresses *what the source means*, it belongs on Signal, not Evidence. Evidence has no `stance`, `sentiment`, `claim`, or `summary` field, and the schema enforces this by construction.

## 3. Traceability rules

These rules are load-bearing — they encode the "no Evidence, no Signal" posture that distinguishes KURO from systems that assert without citing. They should not be relaxed without an explicit decision.

1. **No Evidence, no Signal.** A Signal that cannot cite at least one Evidence id is invalid and is rejected at the schema layer.
2. **No Signal support, no Theme-level conclusion.** A Theme exists only as a grouping of supported Signals.
3. **No Theme/Signal support, no Result-level Inference.** A KURO Result must be reconstructable from its Inference → Themes → Signals → Evidence chain.
4. **Evidence is support, not interpretation.** Evidence carries near-raw material and provenance. Any interpretation, summarization, or stance lives on the Signal — never on Evidence.
5. **Evidence is addressable.** Each Evidence record points back to a specific locatable span within a specific Source Document. "The whole document" is not an acceptable citation.
6. **Confidence at the Signal/Theme/Inference layers is a function of Evidence strength, not a free parameter.** Evidence carries the *inputs* that downstream confidence scoring consumes — it does not carry a confidence value of its own.

Cardinality:

- Source Document → Evidence: **1:N** (a Source Document may yield zero, one, or many Evidence records).
- Evidence → Signal: **N:M** (one Evidence may support multiple Signals; one Signal may cite multiple Evidence records).
- Signal → Theme: **N:1** for MVP (each Signal lives in one Theme).
- Theme → Inference: **N:M**.

Evidence lives once at the **Result level** under `result.evidence`, and Signals reference it by id. Embedding Evidence under each Signal duplicates records and makes contradiction detection harder; the registry shape is intentional.

## 4. Locator

Every Evidence record must carry a `locator` that points back to the span inside its Source Document. The locator is a discriminated union on `kind`; MVP supports three variants:

| `kind` | Use it for | Shape |
|---|---|---|
| `charRange` | Plain-text sources where character offsets are stable (Reddit posts, blog text, scraped review bodies). | `{ kind: "charRange", start: int>=0, end: int>=start }` |
| `lineRange` | Sources where lines are the natural unit (transcripts, code-like content, multi-line quotes). | `{ kind: "lineRange", startLine: int>=1, endLine: int>=startLine }` |
| `anchor` | Sources addressed by stable identifiers rather than offsets (URL fragments, heading slugs, comment ids, post ids). | `{ kind: "anchor", value: non-empty string }` |

Multimodal locators — PDF page coordinates, audio timestamps, video frames, image regions — are deferred. The discriminator field is open, so additional kinds can be added later without a breaking change.

**Locator stability is not promised by the model.** If a Source Document is re-fetched and its text shifts by one character, all `charRange` locators silently break. Handling re-ingestion semantics is the extraction pipeline's job and is out of scope here.

## 5. Extraction provenance

`extraction` is required on every Evidence record:

- `method: "verbatim" | "normalized" | "synthesized"`
  - `verbatim` — the snippet matches the source byte-for-byte.
  - `normalized` — whitespace, case, or punctuation was normalized; `originalSnippet` should be present so reviewers can diff.
  - `synthesized` — the snippet was paraphrased rather than lifted. **Synthesized Evidence must either preserve `originalSnippet` or carry an explicit `qualityHints.notes` explaining why no verbatim anchor exists.** The schema rejects synthesized records that satisfy neither.
- `extractedAt` — ISO-8601 timestamp.
- `extractor` — model name + version or rule id (e.g. `"kuro-extractor@0.1.0"`).

Synthesized Evidence is a loaded category and reviewers should treat it with extra scrutiny. The MVP permits it, gated by the originalSnippet-or-note rule, but the extraction pipeline ticket may revisit whether paraphrase should be forced into the Signal layer instead.

## 6. Quality hints

`qualityHints` is optional and entirely informational. It carries:

- `sourceTrust: "low" | "medium" | "high"` — a coarse label for how much weight downstream confidence should give this Evidence. The enum is intentionally coarse for MVP; a learned or numeric trust signal may replace it later.
- `isDuplicateOf: Id` — points at another Evidence id in the same Result when this record was extracted from the same `(sourceDocumentId, locator)` as another. The Evidence layer never picks a winner between duplicates; both are retained and the marker preserves the link.
- `notes: string` — short reviewer- or extractor-authored note. Required to justify synthesized Evidence without an `originalSnippet`.

Quality hints are **inputs** to downstream confidence scoring (see [CONFIDENCE.md](./CONFIDENCE.md)). Evidence itself does not carry a `confidence` field, by design — confidence is computed from Evidence, not stored on it.

## 7. Edge cases

| Case | Expected behavior |
|---|---|
| **Missing evidence** for a candidate Signal | Signal is dropped. Unsupported Signals are never emitted. |
| **Weak evidence** (single low-trust source, vague snippet) | Signal may be emitted, but its downstream confidence reflects the weakness. Evidence carries `qualityHints.sourceTrust`; the Signal/Theme confidence scoring consumes it. |
| **Duplicated evidence** (same span extracted twice) | Deduplicate by `(sourceDocumentId, locator)`. If two extractions disagree on `snippet` text, keep both and mark one with `qualityHints.isDuplicateOf`. Unmarked duplicates are rejected by the schema. |
| **Contradictory evidence** | Both records are retained. Contradiction is surfaced at the Inference layer ("disagreement"), not resolved at the Evidence layer. Evidence never picks a winner. |
| **Evidence reused across Signals** | Allowed and expected. Evidence is stored once in the Result-level registry; Signals reference by id. |
| **Noisy / low-quality sources** | Evidence is still recorded, with `sourceTrust: "low"`. The Evidence layer does not filter; downstream layers weight. |
| **Synthesized vs. verbatim snippets** | `extraction.method` distinguishes them. Synthesized Evidence must either preserve `originalSnippet` or justify its absence in `qualityHints.notes`. |

## 8. Non-goals

- Defining the confidence scoring function — owned by the confidence model in [CONFIDENCE.md](./CONFIDENCE.md).
- Implementing the extraction pipeline that *produces* Evidence.
- Multimodal locators (PDF coordinates, audio timestamps, video frames, image regions).
- Cross-document Evidence linking or deduplication across Source Documents.
- Versioning Evidence across re-runs of the same Source Document.
- UI or presentation of Evidence in the Result rendering.

## 9. Cross-links

- Conceptual definition: [GLOSSARY.md §Evidence](./GLOSSARY.md#evidence).
- Signal entry and the "no Evidence, no Signal" rule: [GLOSSARY.md §Signal](./GLOSSARY.md#signal).
- Confidence inputs that consume Evidence quality hints: [CONFIDENCE.md §Drivers per level](./CONFIDENCE.md#4-drivers-per-level).
- Schema: [`packages/schemas/src/evidence.ts`](../packages/schemas/src/evidence.ts) and the Evidence-related validation rules in [`packages/schemas/src/result.ts`](../packages/schemas/src/result.ts).
- Source Attribution (the separate provenance layer that records *where* the Source Document came from): [SOURCE_ATTRIBUTION.md](./SOURCE_ATTRIBUTION.md). Evidence cites Source Documents; attribution describes them — the two layers do not collapse.
