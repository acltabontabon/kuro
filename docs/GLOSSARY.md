# KURO Domain Glossary

## 1. Purpose and Scope

This document is the canonical vocabulary for the KURO domain. Every first-class concept that KURO reasons about - Subject, Source Document, Evidence, Signal, Theme, KURO Inference, KURO Result, and Confidence - is defined here in plain language.

All downstream work (schemas, prompts, APIs, UI labels, data pipelines) must conform to the definitions in this document. When a definition here changes, the dependent artifacts change. When a dependent artifact needs a concept this document does not cover, this document is updated first.

This is a conceptual model, not a schema. It contains no JSON, no types, no field names. The schema is a faithful representation of this model and lives in a separate document.

## 2. The KURO Philosophy in the Model

KURO presents informed inferences, not facts, and does not direct user decisions. This constraint is not a disclaimer bolted onto the output - it is encoded in the model itself.

Specifically:

- A **Signal** is an interpreted opinion, not a fact about the world.
- A **Theme** is a cluster of opinions, not a verdict.
- A **KURO Inference** is explicit, in its own definition, about what it may and may not claim (see the Glossary below).
- **Confidence** describes how well-supported a pattern is by the available signals - never how true something is.

If a future addition to the model would require KURO to claim objective truth, verify a fact, predict an outcome, rank subjects against one another, or recommend a decision on behalf of the user, that addition does not belong in the model.

## 3. Concept Hierarchy

```
Source Document
    |  (excerpt extracted from)
    v
Evidence
    |  (interpreted as)
    v
Signal
    |  (clustered into)
    v
Theme
    |  (synthesized into)
    v
KURO Inference
    |  (presented, together with its Themes, as)
    v
KURO Result
```

Each level is derived from the level above it. Each level retains a reference to the level above it, so any item in a KURO Result can be traced back through Inference -> Theme -> Signal -> Evidence -> Source Document.

A **Source Attribution** attaches to each Source Document 1:1 and describes *where it came from* — distinct from Evidence (what was quoted), Confidence (how strongly it supports), and the Source Document itself (what was read). See its entry in the Glossary.

**Traceability rule.** A KURO output that cannot be traced back to Evidence is not a valid KURO output. This applies at every level: a Signal without Evidence is not a Signal, a Theme without supporting Signals is not a Theme, and a KURO Inference that cannot be grounded in its Themes is not a KURO Inference.

**Subject** sits outside this derivation chain. It is the entity the query is about and is supplied by the user; it anchors relevance at every level of the chain. See its entry in the Glossary.

Confidence is a property attached at three levels (Signal, Theme, Result) and is described in the Glossary below.

## 4. Glossary

### Subject

**Definition.** The entity or target that a KURO query is about. A Subject can be a company, an apartment building, a product, a service, a neighborhood or location, a role at an employer, or any comparable real-world target a person would research before making a decision.

**Conceptual requirements.** A Subject must be identifiable well enough that KURO can decide whether a given Source Document is about it. A Subject has a kind (e.g. employer, rental property) which informs what topics are relevant.

**Relationship to neighbors.** Subject is not derived from Source Documents - it is given to KURO as part of the query. Subject anchors relevance for everything downstream: a Source Document is collected because it might pertain to the Subject; Evidence, Signals, Themes, the Inference, and the Result are all about the Subject.

**Example.** "Acme Corp as an employer" or "123 Main Street, Apt 4B as a rental."

---

### Source Document

**Definition.** A piece of publicly available content that contains a person's stated experience, opinion, observation, or report about a Subject (or about something close enough to the Subject to be potentially relevant). A Reddit post, a Reddit comment, a Glassdoor review, an apartment-rating review, a forum thread, a blog post - first-hand or reported personal accounts that people share publicly.

KURO does not treat Source Documents as authoritative; it treats them as expressions of personal experience.

**Conceptual requirements.** A Source Document carries enough information to be located again (where it came from), enough context to be read fairly (the content and its surrounding context), and enough provenance to support freshness and diversity reasoning (when it was published or captured, what platform it came from, who the author is when that is publicly visible).

**Relationship to neighbors.** A Source Document is the upstream root. Zero, one, or many pieces of Evidence may be extracted from a single Source Document. **A Source Document may produce zero Evidence** if it turns out to be irrelevant to the Subject, duplicative of another source, spammy or promotional, too ambiguous to interpret fairly, or outside the scope of the query.

**Example.** A Reddit post titled "Two years at Acme Corp - honest review" captured from r/jobs on a given date.

---

### Source Attribution

**Definition.** A structured record describing the origin of a Source Document: its publishing-surface type, location, collection context, and a coarse reliability hint. Attribution answers "where did this material come from?" — it does not interpret the material, quote it, or weigh it.

**Conceptual requirements.** An attribution carries: a reference to the Source Document it describes (1:1), a coarse `sourceType` (forum, review site, blog, etc.), the URL KURO fetched when one exists, the timestamp at which KURO obtained the material (`fetchedAt`, always required), the source-declared publish time when reliably extractable (`publishedAt`, never inferred), a coarse `trustTier`, and an optional public `authorHandle`. When no URL exists (user paste, file upload, API import), attribution records the access context via `accessedVia` instead. Attribution also carries an optional bounded `metadata` record for non-PII collection hints and an optional `redactions` array describing — by *category*, never by raw value — what was removed.

**Relationship to neighbors.** Attribution attaches to a Source Document 1:1 and stays separate from Evidence and Confidence. Themes and Signals reach attribution only transitively, through `Evidence → Source Document → Source Attribution`. Attribution does not replace Evidence: a Signal that cannot cite Evidence is invalid even when its source is well-attributed.

**Example.** A Glassdoor review of Acme Corp captured on 2026-05-20, with `sourceType: review_site`, `trustTier: secondary`, fetched directly from the public review URL.

For the full Source Attribution rules — enums, validation rules, redaction semantics, the trust-tier boundary, and worked examples — see [SOURCE_ATTRIBUTION.md](./SOURCE_ATTRIBUTION.md).

---

### Trust Tier

**Definition.** A coarse, explicit reliability/context hint attached to a Source Attribution — one of `primary`, `secondary`, `community`, `low_context`, or `unknown`.

**What trust tier is not.** Trust tier is **not** a probability of correctness, **not** a truth label, and **not** a substitute for Evidence or Confidence. It is a provenance hint about the kind of surface the material came from, used for UI rendering and as a debugging label. Any rendering of trust tier must be paired with language that frames it as a reliability hint, never as a truth claim.

**`unknown` requires a rationale.** A coarse "I don't know" must always be reasoned: the attribution must carry a non-empty `trustRationale` explaining why provenance could not be safely classified.

---

### Canonical URL

**Definition.** A normalized form of a Source Attribution's `url` — redirects resolved, tracking parameters stripped (`utm_*`, `fbclid`, `gclid`, `ref`, `mc_cid`, `mc_eid`), host lowercased — used for deduplication and stable references.

**When to omit.** Canonical URL is optional. When safe normalization is not possible, omit it rather than emitting a half-normalized form. Canonical URLs that still carry known tracking parameters are rejected at validation.

---

### Access Context

**Definition.** The mechanism by which KURO obtained source material when no meaningful URL exists — recorded on Source Attribution as `accessedVia` and taking one of `direct_fetch`, `user_paste`, `file_upload`, `api_import`, or `other`. Required when the attribution's `url` is absent.

**Why it exists.** Not all material has a stable public URL. A user-pasted excerpt, a locally uploaded transcript, or content imported via a partner API still needs an auditable provenance record. Access context captures how it arrived without inventing a URL.

---

### Redaction

**Definition.** A recorded removal of a field whose raw value must not be stored or rendered (private identifiers, emails, real names, hidden platform metadata). A Redaction Record preserves the *category* of what was removed (and optionally why), but **never** the raw value itself.

**Why it exists.** Auditability — KURO can answer "did you intentionally drop a field?" — without holding the value that was dropped. The schema enforces this: a Redaction Record's strict shape rejects a `value` key.

---

### Evidence

**Definition.** A traceable, addressable extract from a single Source Document that *could* be used to support one or more Signals. Evidence is raw or minimally-normalized material plus the provenance needed to locate it again in the original source. Evidence does **not** carry interpretation, stance, sentiment, or conclusions — those live on the Signal.

**Conceptual requirements.** Evidence carries the extracted snippet KURO will quote, a reference to the parent Source Document, a **locator** that pinpoints the extract within that document, and extraction provenance (when it was extracted, by what extractor, and whether the snippet is verbatim, normalized, or synthesized). When extraction is synthesized (paraphrased rather than lifted), Evidence must either preserve the verbatim original alongside the snippet or carry an explicit note explaining why no verbatim anchor exists. Optional **quality hints** — source trust, duplicate markers, reviewer notes — describe the strength of the support; they are *inputs* to downstream confidence scoring, never confidence itself.

**Relationship to neighbors.** Evidence is extracted from a Source Document (1:N). One or more pieces of Evidence support each Signal, and a single Evidence record may support multiple Signals (N:M); Signals reference Evidence by id rather than embedding it, so the Evidence registry lives at the Result level. **A Signal whose evidence reference list is empty is not a Signal and is rejected.** Evidence is what KURO shows the user when asked "why do you say that?"

**Example.** The sentence "Management changed three times in my first year and each reorg killed morale" lifted verbatim from the Reddit post above, addressed by a character range, extracted by `kuro-extractor@0.1.0` on 2026-05-20.

For the full Evidence rules — locator kinds, quality hints, edge-case handling, and non-goals — see [EVIDENCE.md](./EVIDENCE.md).

---

### Signal

**Definition.** The smallest interpreted unit in the KURO model - a single opinion or experience extracted from Evidence. A Signal is not the raw text; it is KURO's interpretation of what that text says about the Subject.

**Conceptual requirements.**

- A topic - what the Signal is about (e.g. management stability, commute, noise levels).
- A sentiment - the polarity of the opinion the Signal expresses (positive, negative, neutral, or mixed). Sentiment is an attribute of a Signal; it is not how Signals are organized.
- A claim - a short interpreted statement of what the Signal says about the Subject. The claim is KURO's interpretation of the underlying Evidence, not a verbatim excerpt.
- At least one supporting piece of Evidence. A Signal whose evidence reference list is empty is not a Signal and is rejected at validation. See [EVIDENCE.md](./EVIDENCE.md).
- A source reference (carried via the Evidence), used downstream for diversity and freshness reasoning.
- A Signal-level confidence.

**Sentiment guidance.** A Signal should usually express one dominant sentiment. If a single Evidence excerpt contains separable positive and negative opinions (for example: "great pay but terrible management"), KURO should produce **separate Signals**, not one Mixed Signal. The four sentiment values are distinct and not interchangeable:

- `positive` and `negative` carry the obvious polarity.
- `neutral` is reserved for **informational, factual, or non-emotional** observations - e.g. "the office is on the 14th floor" or "the building uses on-site management." Neutral is not a polite alias for "I don't know"; it means the Signal expresses no evaluative content.
- `mixed` is reserved for opinions that carry **genuine internal tension** and cannot be fairly split into separate Signals (for example: "I have complicated feelings about leaving").

Forcing a neutral observation into `mixed` (or vice versa) is incorrect: `neutral` describes absence of polarity, `mixed` describes presence of competing polarity. Mixed sentiment at the **Theme** level remains first-class and expected.

**Relationship to neighbors.** A Signal is interpreted from Evidence and is grouped with other related Signals to form a Theme.

**Example.** Topic: management stability. Sentiment: negative. Supported by the excerpt above plus two similar excerpts from other reviewers.

---

### Theme

**Definition.** A cluster of related Signals about the same topic. A Theme is the primary organizing concept of a KURO Result: KURO results are organized by topic, not by polarity.

**Conceptual requirements.**

- A topic name (e.g. Work-Life Balance, Compensation, Building Maintenance).
- The set of Signals that belong to the Theme.
- An aggregate sentiment for the Theme - Positive, Negative, Neutral, or Mixed - derived from the sentiments of its Signals. Mixed is a first-class value at the Theme level, not an error state. Neutral is reserved for themes that aggregate purely informational Signals (see Signal sentiment guidance); it is not interchangeable with Mixed.
- A Theme-level confidence.
- A theme-scoped **interpretation layer**, expressed as three structured fields:
  - **maySuggest** - short claims describing what this Theme *may* suggest about the Subject, given its Signals.
  - **mayNotSuggest** - short claims explicitly disclaiming what this Theme should *not* be read to suggest.
  - **limitations** - caveats specific to this Theme (e.g. low source diversity, single supporting Signal, stale Evidence).

  A theme-scoped claim does not carry `themeIds` - the parent Theme is the traceability boundary. A separate prose `interpretation` field is intentionally **not** part of the model: the structured trio above already covers the same ground, and shipping both invites duplication.

**Relationship to neighbors.** A Theme is composed of Signals. A KURO Result is composed of Themes (together with a KURO Inference derived from them).

**Example.** Theme "Management Stability (Negative)" containing seven Signals drawn from five different sources, all describing frequent reorgs and turnover.

---

### KURO Inference

**Definition.** The informed perspective KURO derives from the Themes and Signals for a given Subject. The Inference is not a list of findings - it is what KURO says about those findings.

**A KURO Inference may:**

- Identify patterns across the collected Signals.
- Surface consensus where Signals agree.
- Surface disagreement where Signals conflict.
- Summarize the overall community sentiment about the Subject.

**A KURO Inference may not:**

- Claim objective truth.
- Verify facts.
- Predict outcomes.
- Rank Subjects as objectively better or worse than one another. Ranking is a disguised recommendation and is excluded for the same reason as recommendations.
- Recommend a decision on behalf of the user.

This distinction is central to KURO's philosophy and is enforced at the model level: if a candidate output crosses into any of the "may not" categories, it does not belong in the Inference.

**Relationship to neighbors.** The Inference is derived from the full set of Themes and their Signals. The Inference is one component of a KURO Result.

---

### KURO Result

**Definition.** The final, user-facing output for a single KURO query about a Subject. A coherent presentation of what the community is saying about that Subject.

Every Result declares a **Decision Category** that bounds its scope and interpretation. MVP supports only `Employment Intelligence` and `Rental Intelligence`; see [DECISION_CATEGORIES.md](./DECISION_CATEGORIES.md).

A KURO Result is composed of **two parts**: the set of Themes, and the KURO Inference derived from those Themes. A Result is not just a list of Themes - it is Themes plus the informed perspective synthesized over them.

**Conceptual requirements.**

- The Subject of the query.
- An **outcome** - either `ok` (KURO formed a meaningful inference) or `insufficient_data` (KURO could not). See "Insufficient data" below.
- A **summary** - a short, plain-language overview of what KURO *observed*. The summary stays close to the source-backed Signals; it describes, it does not synthesize.
- The set of Themes.
- The KURO Inference derived from those Themes.
- A **source summary** - an aggregated description of the evidence base. At minimum it answers: how many Source Documents were considered, what source types / platforms were represented, whether any sources were excluded and why, the freshness window of what was read, and any narrow / stale / low-diversity notes that bear on how to read the Result.
- A Result-level confidence.
- A **final KURO** - a short closing synthesis expressed in may / may-not framing. Unlike `summary`, this *is* synthesis: it is what KURO cautiously infers from the Themes and Inference. The final KURO must **not** become advice (no "you should apply / avoid / sign / decline"); it summarizes what the Result may and may not suggest.
- Traceability - every Theme links to its Signals; every Signal links to its Evidence; every piece of Evidence links to its Source Document. A Result that breaks this chain at any point is not a valid Result.

**`summary` vs `finalKuro`.** These are distinct on purpose: `summary` is descriptive ("here is what was observed"); `finalKuro` is interpretive but bounded ("here is what may and may not be inferred"). Collapsing them would erase the line between source-backed observation and KURO synthesis, which is exactly the line KURO's posture depends on.

**Insufficient data.** A valid KURO Result may report `outcome: insufficient_data` when there is not enough usable community material to support an inference. In that branch:

- Themes, Signals, and Evidence may be empty.
- Source Documents may be empty **only** if no usable document was found or read. If KURO did read documents but they were insufficient (stale, duplicate, irrelevant, spammy, low-diversity), those documents stay in the Result and the reason is explained via the source summary's exclusions and diversity notes and via the Inference's limitations.
- The Inference's limitations must be non-empty and must explain why KURO cannot infer responsibly.
- The Result-level confidence rating must be `low` or `unknown` (see Confidence).
- The final KURO must say clearly that KURO cannot form a meaningful community inference from the available material.

Insufficient data is a successful Result, not a transport error. The request succeeded; the analytical outcome is "not enough to say."

**Relationship to neighbors.** The Result is the terminal node. It is what KURO shows the user.

---

### Decision Category

**Definition.** The supported scope KURO operates in for a given Result. Every Result declares exactly one Decision Category, which tells downstream consumers what kind of subject the Result is about and how the Result should be interpreted.

Decision Category controls **scope and interpretation**. It is not an outcome, not a confidence score, and not a truth claim. It does not bypass Evidence, Confidence, or Source Attribution requirements.

**MVP categories.** KURO MVP supports exactly two categories:

- **Employment Intelligence** — see below.
- **Rental Intelligence** — see below.

All other domains (banking, healthcare, insurance, schools / education admissions, consumer products, legal eligibility, creditworthiness, medical suitability, financial advice) are out of MVP scope and are rejected by the schema. Adding a category is a deliberate design action, not a configuration change. See [DECISION_CATEGORIES.md](./DECISION_CATEGORIES.md) for the full list, covers / does-not-cover details, language guardrails, and edge cases.

**Category is set by the producer / caller / pipeline.** It is not inferred from documents. One Result has exactly one category.

---

### Employment Intelligence

**Definition.** The Decision Category covering cautious, evidence-backed signals about employers, workplaces, and work experiences, drawn from public/community feedback. Topics in scope include employer reputation, workplace culture, compensation sentiment, management sentiment, work-life balance, hiring/interview experience, attrition/retention signals, and career growth sentiment.

**Not for hiring decisions.** Employment Intelligence does not decide whether to hire a specific person, does not support employment background checks, and does not make protected-class inferences. It does not give legal employment advice or salary guarantees. See [DECISION_CATEGORIES.md](./DECISION_CATEGORIES.md).

---

### Rental Intelligence

**Definition.** The Decision Category covering cautious, evidence-backed signals about landlords, property managers, buildings, properties, neighborhoods, and rental experiences, drawn from public/community feedback. Topics in scope include landlord/property-manager reputation, building/property sentiment, maintenance responsiveness, safety/noise/cleanliness sentiment, neighborhood/community feedback, rental experience patterns, and tenant experience themes.

**Not for tenant screening.** Rental Intelligence does not decide whether to approve a tenant, does not support tenant screening or credit checks, and does not make protected-class inferences. It does not give legal housing advice. See [DECISION_CATEGORIES.md](./DECISION_CATEGORIES.md).

---

### Confidence

**Definition.** A measure of how well-supported a pattern is by the available material. Confidence describes the **strength of support**, never the truth, of what is being shown. A `high` confidence Theme means "the Signals supporting this Theme are numerous, diverse, recent, and consistent"; it does not mean "this claim about the Subject is objectively true."

**Confidence applies at three levels:**

- **Signal confidence.** How confidently KURO can say the interpretation matches the Evidence. Driven by: clarity of the excerpt, ambiguity of the language, and how directly the Evidence supports the interpreted opinion.
- **Theme confidence.** How well-supported a Theme is by its Signals. Driven by: source count (how many Signals support the Theme), source diversity (how many distinct Source Documents and platforms), source freshness (how recent the underlying Source Documents are), and **signal consistency** — how much the Signals agree on the **claim content** (what is being said about the Subject), *not* on sentiment polarity. A Theme with `sentiment: "mixed"` where each side internally agrees on its claims is consistent for confidence purposes; consistency only drops when Signals disagree on facts or interpretations.
- **Result confidence.** How well-supported the overall picture is. Driven by the Theme confidences and the breadth of topics covered relative to what one would expect for the Subject.

Each level composes from the level below it. A Result composed of a single high-confidence Theme is not a high-confidence Result, because breadth is a separate input.

**Confidence ratings.** The qualitative rating is one of `low`, `medium`, or `high` at the Signal and Theme levels. The Result level adds a fourth value, `unknown`. `unknown` is **Result-level only** — Signals and Themes that cannot be rated should not be emitted at all (a Signal exists only because Evidence supports it; a Theme exists only because at least one Signal supports it).

The distinction between `low` and `unknown` at the Result level matters:

- Use `unknown` when there is no usable evidence to score against — KURO cannot responsibly assign even a low rating.
- Use `low` when there *is* some evidence but it is weak, narrow, stale, or contradictory.

For an insufficient-data Result, prefer `unknown` if KURO could not extract any usable Evidence at all, and `low` if KURO did extract some but it was not enough to sustain an inference.

For the MVP rules — bands, drivers, situation handling, freshness window, breadth cap, and the `reasons` contract — see [CONFIDENCE.md](./CONFIDENCE.md).

## 5. Decision: Why Not Positive/Negative as the Primary Structure

KURO Results are organized by Theme (topic), with sentiment as an attribute of each Theme. They are not organized as two top-level buckets of "Positive Signals" and "Negative Signals."

Reasons:

1. **Real experiences are mixed.** A workplace can have great compensation and poor management. A building can have a quiet location and an unresponsive landlord. A polarity-first structure forces the model to split a single coherent topic across two buckets.
2. **Polarity-first encodes judgment into the schema.** Telling the user "here are the negatives" implies KURO has decided what counts as bad. Topic-first with sentiment as an attribute lets the user read the nuance themselves - consistent with KURO's philosophy of presenting, not deciding.
3. **Mixed becomes a first-class value at the Theme level.** With topic-first Themes, a Theme can be Mixed and carry both sides of the conversation in one place, which is how most real experiences actually present.

The resulting shape of a Result is therefore a list like "Work-Life Balance (Negative)", "Compensation (Positive)", "Management (Mixed)" - not two columns of pros and cons.

## 6. Downstream Use

The schema ticket, and any subsequent data-model or API ticket, should be authored as a faithful representation of this model. If a schema needs a field that has no corresponding concept here, this document is updated first. If a concept here cannot be represented in the schema without distortion, the schema is reshaped - not the concept.
