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

### Evidence

**Definition.** A specific excerpt or snippet from a Source Document that supports a Signal. Evidence is the citable link between an interpreted Signal and the raw content it came from.

**Conceptual requirements.** Evidence carries the verbatim excerpt, a reference to the parent Source Document, enough positional information for the excerpt to be located again, and any minimal surrounding context needed to read the excerpt fairly.

**Relationship to neighbors.** Evidence is extracted from a Source Document. One or more pieces of Evidence support each Signal. Evidence is what KURO shows the user when asked "why do you say that?"

**Example.** The sentence "Management changed three times in my first year and each reorg killed morale" lifted from the Reddit post above.

---

### Signal

**Definition.** The smallest interpreted unit in the KURO model - a single opinion or experience extracted from Evidence. A Signal is not the raw text; it is KURO's interpretation of what that text says about the Subject.

**Conceptual requirements.**

- A topic - what the Signal is about (e.g. management stability, commute, noise levels).
- A sentiment - the polarity of the opinion the Signal expresses (positive, negative, or mixed). Sentiment is an attribute of a Signal; it is not how Signals are organized.
- At least one supporting piece of Evidence.
- A source reference (carried via the Evidence), used downstream for diversity and freshness reasoning.
- A Signal-level confidence.

**Sentiment guidance.** A Signal should usually express one dominant sentiment. If a single Evidence excerpt contains separable positive and negative opinions (for example: "great pay but terrible management"), KURO should produce **separate Signals**, not one Mixed Signal. Mixed sentiment at the Signal level is reserved for opinions that are genuinely ambivalent and cannot be fairly split (for example: "I have complicated feelings about leaving"). Mixed sentiment at the **Theme** level remains first-class and expected.

**Relationship to neighbors.** A Signal is interpreted from Evidence and is grouped with other related Signals to form a Theme.

**Example.** Topic: management stability. Sentiment: negative. Supported by the excerpt above plus two similar excerpts from other reviewers.

---

### Theme

**Definition.** A cluster of related Signals about the same topic. A Theme is the primary organizing concept of a KURO Result: KURO results are organized by topic, not by polarity.

**Conceptual requirements.**

- A topic name (e.g. Work-Life Balance, Compensation, Building Maintenance).
- The set of Signals that belong to the Theme.
- An aggregate sentiment for the Theme - Positive, Negative, or Mixed - derived from the sentiments of its Signals. Mixed is a first-class value at the Theme level, not an error state.
- A Theme-level confidence.

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

A KURO Result is composed of **two parts**: the set of Themes, and the KURO Inference derived from those Themes. A Result is not just a list of Themes - it is Themes plus the informed perspective synthesized over them.

**Conceptual requirements.**

- The Subject of the query.
- The set of Themes.
- The KURO Inference derived from those Themes.
- A Result-level confidence.
- Traceability - every Theme links to its Signals; every Signal links to its Evidence; every piece of Evidence links to its Source Document. A Result that breaks this chain at any point is not a valid Result.

**Relationship to neighbors.** The Result is the terminal node. It is what KURO shows the user.

---

### Confidence

**Definition.** A measure of how well-supported a pattern is by the available material. Confidence describes the support, never the truth, of what is being shown.

**Confidence applies at three levels:**

- **Signal confidence.** How confidently KURO can say the interpretation matches the Evidence. Driven by: clarity of the excerpt, ambiguity of the language, and how directly the Evidence supports the interpreted opinion.
- **Theme confidence.** How well-supported a Theme is by its Signals. Driven by: source count (how many Signals support the Theme), source diversity (how many distinct Source Documents and platforms), source freshness (how recent the underlying Source Documents are), and signal consistency (how much the Signals agree on sentiment and detail).
- **Result confidence.** How well-supported the overall picture is. Driven by the Theme confidences and the breadth of topics covered relative to what one would expect for the Subject.

Each level composes from the level below it. A Result composed of a single high-confidence Theme is not a high-confidence Result, because breadth is a separate input.

## 5. Decision: Why Not Positive/Negative as the Primary Structure

KURO Results are organized by Theme (topic), with sentiment as an attribute of each Theme. They are not organized as two top-level buckets of "Positive Signals" and "Negative Signals."

Reasons:

1. **Real experiences are mixed.** A workplace can have great compensation and poor management. A building can have a quiet location and an unresponsive landlord. A polarity-first structure forces the model to split a single coherent topic across two buckets.
2. **Polarity-first encodes judgment into the schema.** Telling the user "here are the negatives" implies KURO has decided what counts as bad. Topic-first with sentiment as an attribute lets the user read the nuance themselves - consistent with KURO's philosophy of presenting, not deciding.
3. **Mixed becomes a first-class value at the Theme level.** With topic-first Themes, a Theme can be Mixed and carry both sides of the conversation in one place, which is how most real experiences actually present.

The resulting shape of a Result is therefore a list like "Work-Life Balance (Negative)", "Compensation (Positive)", "Management (Mixed)" - not two columns of pros and cons.

## 6. Downstream Use

The schema ticket, and any subsequent data-model or API ticket, should be authored as a faithful representation of this model. If a schema needs a field that has no corresponding concept here, this document is updated first. If a concept here cannot be represented in the schema without distortion, the schema is reshaped - not the concept.
