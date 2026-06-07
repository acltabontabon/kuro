# KURO Domain Glossary

## 1. Purpose & Scope

This document is the canonical vocabulary for the KURO domain. Every first-class concept that KURO reasons about — Source Document, Evidence, Signal, Theme, KURO Inference, KURO Result, and Confidence — is defined here in plain language.

All downstream work (schemas, prompts, APIs, UI labels, data pipelines) must conform to the definitions in this document. When a definition here changes, the dependent artifacts change. When a dependent artifact needs a concept this document does not cover, this document is updated first.

This is a conceptual model, not a schema. It contains no JSON, no types, no field names. The schema is a faithful representation of this model and lives in a separate document.

## 2. The KURO Philosophy in the Model

KURO presents informed inferences, not facts, and does not direct user decisions. This constraint is not a disclaimer bolted onto the output — it is encoded in the model itself.

Specifically:

- A **Signal** is an *interpreted opinion*, not a fact about the world.
- A **Theme** is a *cluster of opinions*, not a verdict.
- A **KURO Inference** is explicit, in its own definition, about what it may and may not claim (see §4).
- **Confidence** describes how well-supported a pattern is by the available signals — never how *true* something is.

If a future addition to the model would require KURO to claim objective truth, verify a fact, or recommend a decision on behalf of the user, that addition does not belong in the model.

## 3. Concept Hierarchy

```
Source Document
    │  (excerpt extracted from)
    ▼
Evidence
    │  (interpreted as)
    ▼
Signal
    │  (clustered into)
    ▼
Theme
    │  (synthesized into)
    ▼
KURO Inference
    │  (presented as)
    ▼
KURO Result
```

Each level is derived from the level above it. Each level retains a reference to the level above it, so any item in a KURO Result can be traced back through Inference → Theme → Signal → Evidence → Source Document.

Confidence is a property attached at three levels (Signal, Theme, Result) and is described in §4.

## 4. Glossary

### Source Document

**Definition.** A piece of raw, publicly available content collected by KURO. A Reddit post, a Reddit comment, a Glassdoor review, an apartment-rating review, a forum thread, a news or blog article — anything that contains first-hand or reported experience.

**Required properties.** Origin URL or stable identifier, source platform, original author handle (when public), publication or capture timestamp, raw content, language.

**Relationship to neighbors.** A Source Document is the upstream root. Zero, one, or many pieces of Evidence may be extracted from a single Source Document.

**Example.** A Reddit post titled *"Two years at Acme Corp — honest review"* captured from r/jobs on a given date.

---

### Evidence

**Definition.** A specific excerpt or snippet from a Source Document that supports a Signal. Evidence is the citable link between an interpreted Signal and the raw content it came from.

**Required properties.** Reference to the parent Source Document, the verbatim excerpt, position within the source (so the excerpt can be located and re-shown), and any minimal surrounding context needed to read the excerpt fairly.

**Relationship to neighbors.** Evidence is extracted from a Source Document. One or more pieces of Evidence support each Signal. Evidence is what KURO shows the user when asked *"why do you say that?"*

**Example.** The sentence *"Management changed three times in my first year and each reorg killed morale"* lifted from the Reddit post above.

---

### Signal

**Definition.** The smallest interpreted unit in the KURO model — a single opinion or experience extracted from Evidence. A Signal is not the raw text; it is KURO's interpretation of what that text says.

**Required properties.**

- Topic — what the Signal is about (e.g. *management stability*, *commute*, *noise levels*).
- Sentiment — the polarity of the opinion the Signal expresses (positive, negative, or mixed). Sentiment is an *attribute* of a Signal; it is not how Signals are organized.
- Supporting Evidence — at least one piece of Evidence the Signal was extracted from.
- Source reference — derived from the Evidence, used for diversity and freshness calculations.
- Signal confidence — see §4 *Confidence*.

**Relationship to neighbors.** A Signal is interpreted from Evidence and is grouped with other related Signals to form a Theme.

**Example.** *Topic: management stability. Sentiment: negative.* Supported by the excerpt above plus two similar excerpts from other reviewers.

---

### Theme

**Definition.** A cluster of related Signals about the same topic. A Theme is the primary organizing concept of a KURO Result: KURO results are organized by topic, not by polarity.

**Required properties.**

- Topic name (e.g. *Work-Life Balance*, *Compensation*, *Building Maintenance*).
- The set of Signals that belong to the Theme.
- Aggregate sentiment for the Theme — *Positive*, *Negative*, or *Mixed* — derived from the sentiments of its Signals. *Mixed* is a first-class value, not an error state.
- Theme confidence — see §4 *Confidence*.

**Relationship to neighbors.** A Theme is composed of Signals. A KURO Result is composed of Themes.

**Example.** Theme *Management Stability (Negative)* containing seven Signals drawn from five different sources, all describing frequent reorgs and turnover.

---

### KURO Inference

**Definition.** The informed perspective KURO derives from the Themes and Signals for a given subject. The Inference is not a list of findings — it is what KURO says *about* those findings.

**A KURO Inference may:**

- Identify patterns across the collected Signals.
- Surface consensus where Signals agree.
- Surface disagreement where Signals conflict.
- Summarize the overall community sentiment about the subject.

**A KURO Inference may not:**

- Claim objective truth.
- Verify facts.
- Predict outcomes.
- Recommend a decision on behalf of the user.

This distinction is central to KURO's philosophy and is enforced at the model level: if a candidate output crosses into any of the four "may not" categories, it does not belong in the Inference.

**Relationship to neighbors.** The Inference is derived from the full set of Themes and their Signals. The Inference is one component of a KURO Result.

---

### KURO Result

**Definition.** The final, user-facing output for a single KURO query. A coherent presentation of what the community is saying about a subject.

**Required properties.**

- The subject of the query (e.g. *Acme Corp as an employer*, *123 Main St as a rental*).
- The set of Themes.
- The KURO Inference.
- Result confidence — see §4 *Confidence*.
- Traceability — every Theme links to its Signals; every Signal links to its Evidence; every piece of Evidence links to its Source Document.

**Relationship to neighbors.** The Result is the terminal node. It is what KURO shows the user.

---

### Confidence

**Definition.** A measure of how well-supported a pattern is by the available material. Confidence describes the *support*, never the *truth*, of what is being shown.

**Confidence applies at three levels:**

- **Signal confidence.** How confidently KURO can say the interpretation matches the Evidence. Driven by: clarity of the excerpt, ambiguity of the language, and how directly the Evidence supports the interpreted opinion.
- **Theme confidence.** How well-supported a Theme is by its Signals. Driven by: source count (how many Signals support the Theme), source diversity (how many distinct Source Documents and platforms), source freshness (how recent the underlying Source Documents are), and signal consistency (how much the Signals agree on sentiment and detail).
- **Result confidence.** How well-supported the overall picture is. Driven by the Theme confidences and the breadth of topics covered relative to what one would expect for the subject.

Each level composes from the level below it. A Result composed of a single high-confidence Theme is *not* a high-confidence Result, because breadth is a separate input.

## 5. Decision: Why Not Positive/Negative as the Primary Structure

KURO Results are organized by Theme (topic), with sentiment as an attribute of each Theme. They are not organized as two top-level buckets of *Positive Signals* and *Negative Signals*.

Reasons:

1. **Real experiences are mixed.** A workplace can have great compensation and poor management. A building can have a quiet location and an unresponsive landlord. A polarity-first structure forces the model to split a single coherent topic across two buckets.
2. **Polarity-first encodes judgment into the schema.** Telling the user *"here are the negatives"* implies KURO has decided what counts as bad. Topic-first with sentiment as an attribute lets the user read the nuance themselves — consistent with KURO's philosophy of presenting, not deciding.
3. **Mixed becomes a first-class value.** With topic-first Themes, a Theme can be *Mixed* and carry both sides of the conversation in one place, which is how most real experiences actually present.

The resulting shape of a Result is therefore a list like *Work-Life Balance (Negative)*, *Compensation (Positive)*, *Management (Mixed)* — not two columns of pros and cons.

## 6. Downstream Use

The schema ticket, and any subsequent data-model or API ticket, should be authored as a faithful representation of this model. If a schema needs a field that has no corresponding concept here, this document is updated first. If a concept here cannot be represented in the schema without distortion, the schema is reshaped — not the concept.
