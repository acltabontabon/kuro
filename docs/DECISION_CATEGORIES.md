# KURO Decision Categories (MVP)

## 1. Purpose

A **Decision Category** is the supported scope KURO operates in for a given Result. It tells downstream consumers — prompts, UI, API clients, reviewers — what kind of subject the Result is about and how the Result should be interpreted.

Category controls **scope and interpretation**. It does not assert truth. It is not a confidence score. It is not a user decision outcome.

The MVP supports only two categories. Everything else is explicitly out of scope. This is intentional. KURO's posture (cautious, evidence-backed, advisory) does not survive being stretched into regulated or high-stakes decision domains without dedicated work, and the MVP does not have that work.

## 2. Supported MVP categories

KURO MVP supports two categories, and only two:

- `employment_intelligence`
- `rental_intelligence`

These are encoded as a closed Zod enum in `@kuro/schemas` (`DecisionCategory`) and as a required field on `KuroResult` (`category`). Any other value fails schema validation.

### `employment_intelligence`

Cautious, evidence-backed signals about **employers, workplaces, and work experiences**, drawn from public/community feedback.

**Covers:**

- employer reputation
- workplace culture
- compensation sentiment
- management sentiment
- work-life balance
- hiring/interview experience
- attrition/retention signals
- career growth sentiment

**Does not cover:**

- deciding whether to hire a specific person
- employment background checks
- protected-class inference (race, gender, age, disability, religion, etc.)
- legal employment advice
- salary negotiation guarantees
- definitive claims about an employer

### `rental_intelligence`

Cautious, evidence-backed signals about **landlords, property managers, buildings, properties, neighborhoods, and rental experiences**, drawn from public/community feedback.

**Covers:**

- landlord / property manager reputation
- building / property sentiment
- maintenance responsiveness
- safety / noise / cleanliness sentiment
- neighborhood / community feedback
- rental experience patterns
- tenant experience themes

**Does not cover:**

- deciding whether to approve a tenant
- tenant screening
- credit checks
- legal housing advice
- protected-class inference
- definitive claims about a landlord, tenant, building, or neighborhood

## 3. MVP non-goals (explicitly unsupported)

KURO MVP **does not** support, and the schema **rejects**, results in any of the following domains:

- `banking` / financial services
- `healthcare` / medical
- `insurance` / underwriting
- `schools` / education admissions
- `consumer_products` / product reviews
- `legal_eligibility`
- `creditworthiness`
- `medical_suitability`
- `financial_advice`

KURO is not a generic "review intelligence" platform. Adding a new category is not a configuration change — it requires a dedicated design issue with its own non-goals, guardrails, and language review.

## 4. Language guardrails

Within any supported category, inference text and prompts must keep KURO's advisory voice.

Use cautious framing:

- "may indicate"
- "may suggest"
- "evidence base suggests"
- "reported pattern"
- "community sentiment"

Avoid decision verbs:

- "recommend hire"
- "approve tenant"
- "reject applicant"
- "diagnose"
- "underwrite"
- "score eligibility"
- "qualifies"

Avoid definitive claims about specific people or named entities beyond what [Evidence](./EVIDENCE.md), [Confidence](./CONFIDENCE.md), and [Source Attribution](./SOURCE_ATTRIBUTION.md) can support.

## 5. What category is not

- **Category is not an outcome.** It does not encode a user decision (hire / approve / reject / sign / decline). KURO never produces user decisions.
- **Category does not assert truth.** It bounds scope; it does not claim that the contents of the Result are factually correct.
- **Category does not bypass other rules.** Evidence, Confidence, and Source Attribution requirements apply in full to every category. A category does not unlock stronger claims or relaxed citation.

## 6. Edge cases

- **Category is set by the producer / caller / pipeline**, not inferred from documents. A source document that mentions both a workplace and a rental does not change a Result's category.
- **One Result has exactly one category.** Mixing categories in a single Result is not supported.
- **Cross-domain leakage** inside free text (an `employment_intelligence` Result drifting into housing eligibility, or vice versa) is a prompt and review concern. The schema cannot catch this; reviewers must.
- **Protected-class inference remains disallowed** even when source Evidence mentions protected attributes.
- **Named-entity claims** still require Evidence, Confidence, and Source Attribution support. Category does not relax these.
- **Unsupported and missing categories** are schema errors. There is no fallback and no default.
- **Future categories** require their own design issue, non-goals, and guardrails before being added to the enum. The enum stays closed.

## 7. Related

- [GLOSSARY.md](./GLOSSARY.md) — domain vocabulary, including the Decision Category entry.
- [CONFIDENCE.md](./CONFIDENCE.md) — how Result, Theme, and Signal confidence work within any category.
- [EVIDENCE.md](./EVIDENCE.md) — the Evidence requirements that apply regardless of category.
- [SOURCE_ATTRIBUTION.md](./SOURCE_ATTRIBUTION.md) — the attribution requirements that apply regardless of category.
- [TRUST_AND_TRANSPARENCY.md](./TRUST_AND_TRANSPARENCY.md) — Principle 9, "KURO is transparent about scope": unsupported categories are refused as scope issues via the `unsupported_category` result, never forced into generic inference.
