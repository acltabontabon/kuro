import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { KuroResult, lintKuroResultWording } from "../src/index.js";

// @ts-ignore
const here = dirname(fileURLToPath(import.meta.url));

const POSITIVE_EXAMPLES = [
  "employer-acme.json",
  "rental-123-main.json",
  "insufficient-data.json",
  "employment-no-sources.json",
  "employment-partial.json",
  "rental-unusable-sources.json",
  "result.invalid-category.json",
  "result.employment.json",
  "result.rental.json",
  // Trust & transparency fixtures (issue #8)
  "employment-strong.json",
  "employment-mixed.json",
  "rental-limited.json",
  "insufficient-refusal.json",
  "unsupported-category.json",
  "forbidden-wording.json",
];

// KURO-authored prose that must stay clean against the wording lint. The
// forbidden-wording fixture is intentionally excluded — it is the negative
// case checked separately below.
const WORDING_CLEAN_EXAMPLES = POSITIVE_EXAMPLES.filter(
  (n) => n !== "forbidden-wording.json",
);

let failures = 0;

function load(name: string): unknown {
  return JSON.parse(readFileSync(join(here, name), "utf8"));
}

function expectOk(name: string) {
  const data = load(name);
  const r = KuroResult.safeParse(data);
  if (!r.success) {
    failures++;
    console.error(`FAIL  ${name}: expected to parse, got errors:`);
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log(`OK    ${name}`);
  }
}

function expectFailWithPath(label: string, data: unknown, pathFragment: string) {
  const r = KuroResult.safeParse(data);
  if (r.success) {
    failures++;
    console.error(`FAIL  ${label}: expected validation failure but it parsed`);
    return;
  }
  const hit = r.error.issues.some((i) => i.path.join(".").includes(pathFragment));
  if (!hit) {
    failures++;
    console.error(
      `FAIL  ${label}: expected an error path containing "${pathFragment}", got: ${r.error.issues
        .map((i) => i.path.join("."))
        .join(", ")}`,
    );
  } else {
    console.log(`OK    ${label} (rejected as expected)`);
  }
}

function expectFail(label: string, data: unknown) {
  const r = KuroResult.safeParse(data);
  if (r.success) {
    failures++;
    console.error(`FAIL  ${label}: expected validation failure but it parsed`);
  } else {
    console.log(`OK    ${label} (rejected as expected)`);
  }
}

for (const name of POSITIVE_EXAMPLES) {
  expectOk(name);
}

const base = load("employer-acme.json") as Record<string, unknown>;
const partial = load("employment-partial.json") as Record<string, unknown>;
const insufficient = load("insufficient-data.json") as Record<string, unknown>;
const unusable = load("rental-unusable-sources.json") as Record<string, unknown>;
const refusal = load("result.invalid-category.json") as Record<string, unknown>;
const noSources = load("employment-no-sources.json") as Record<string, unknown>;

function clone(src: Record<string, unknown> = base): any {
  return JSON.parse(JSON.stringify(src));
}

// ============================================================
//   Referential / graph checks (sufficient + partial arms)
// ============================================================

{
  const bad = clone();
  bad.signals[0].evidenceIds[0] = "ev_does_not_exist";
  expectFailWithPath("Signal -> missing evidenceId", bad, "evidenceIds");
}
{
  const bad = clone();
  bad.evidence[0].sourceDocumentId = "src_does_not_exist";
  expectFailWithPath("Evidence -> missing sourceDocumentId", bad, "sourceDocumentId");
}
{
  const bad = clone();
  bad.themes[0].signalIds = ["sig_does_not_exist"];
  expectFailWithPath("Theme -> missing signalId", bad, "signalIds");
}
{
  const bad = clone();
  bad.inference.patterns[0].themeIds = ["theme_does_not_exist"];
  expectFailWithPath("Inference -> missing themeId", bad, "themeIds");
}
{
  const bad = clone();
  bad.themes[0].signalIds = [];
  expectFailWithPath("Theme.signalIds must be non-empty", bad, "signalIds");
}
{
  const bad = clone();
  bad.signals.push({ ...bad.signals[0] });
  expectFailWithPath("Duplicate signal id rejected", bad, "signals");
}
{
  const bad = clone();
  bad.themes.push({ ...bad.themes[0] });
  expectFailWithPath("Duplicate theme id rejected", bad, "themes");
}
{
  const bad = clone();
  bad.inference.maySuggest[0].themeIds = ["theme_does_not_exist"];
  expectFailWithPath("Inference.maySuggest -> missing themeId", bad, "maySuggest");
}

// ============================================================
//   `sufficient` arm — required-shape checks
// ============================================================

{
  const bad = clone();
  bad.themes = [];
  expectFailWithPath("sufficient with empty themes rejected", bad, "themes");
}
{
  const bad = clone();
  bad.sourceDocuments = [];
  expectFailWithPath("sufficient with empty sourceDocuments rejected", bad, "sourceDocuments");
}
{
  const bad = clone();
  bad.confidence.rating = "unknown";
  expectFailWithPath("sufficient with confidence.rating=unknown rejected", bad, "confidence.rating");
}
{
  const bad = clone();
  bad.confidence.rating = "high";
  // employer-acme has only 2 themes at medium+ (high, medium, low) — must fail breadth cap.
  expectFailWithPath(
    "sufficient with rating=high and <3 themes at medium+ rejected (breadth cap)",
    bad,
    "confidence.rating",
  );
}

// ============================================================
//   `partial` arm — required new fields and caps
// ============================================================

{
  const bad = clone(partial);
  bad.evidenceGaps = [];
  expectFailWithPath("partial with empty evidenceGaps rejected", bad, "evidenceGaps");
}
{
  const bad = clone(partial);
  delete bad.evidenceGaps;
  expectFailWithPath("partial without evidenceGaps rejected", bad, "evidenceGaps");
}
{
  const bad = clone(partial);
  bad.confidence.rating = "high";
  expectFailWithPath("partial with confidence.rating=high rejected", bad, "confidence.rating");
}
{
  const bad = clone(partial);
  bad.confidence.rating = "unknown";
  expectFailWithPath("partial with confidence.rating=unknown rejected", bad, "confidence.rating");
}
{
  const bad = clone(partial);
  bad.inference.limitations = [];
  expectFailWithPath(
    "partial with empty inference.limitations rejected",
    bad,
    "inference.limitations",
  );
}

// ============================================================
//   `insufficient` arm — forbidden output / required fields
// ============================================================

{
  const bad = clone(insufficient);
  bad.confidence.rating = "high";
  expectFailWithPath(
    "insufficient with confidence.rating=high rejected",
    bad,
    "confidence.rating",
  );
}
{
  const bad = clone(insufficient);
  bad.confidence.rating = "medium";
  expectFailWithPath(
    "insufficient with confidence.rating=medium rejected",
    bad,
    "confidence.rating",
  );
}
{
  const bad = clone(insufficient);
  bad.suggestedNextSources = [];
  expectFailWithPath(
    "insufficient with empty suggestedNextSources rejected",
    bad,
    "suggestedNextSources",
  );
}
{
  const bad = clone(insufficient);
  delete bad.suggestedNextSources;
  expectFailWithPath(
    "insufficient without suggestedNextSources rejected",
    bad,
    "suggestedNextSources",
  );
}
{
  const bad = clone(insufficient);
  delete bad.insufficientDataReason;
  expectFailWithPath(
    "insufficient without insufficientDataReason rejected",
    bad,
    "insufficientDataReason",
  );
}
{
  const bad = clone(insufficient);
  bad.themes = [{ id: "theme_x", topic: "x", sentiment: "neutral", signalIds: ["sig_x"], confidence: { level: "theme", rating: "low", inputs: {}, reasons: [{ driver: "sourceCount", effect: "lowers", note: "n/a" }] }, maySuggest: [], mayNotSuggest: [], limitations: [] }];
  expectFail(
    "insufficient with themes field rejected (discriminated union / strict)",
    bad,
  );
}
{
  const bad = clone(insufficient);
  bad.signals = [{ id: "sig_x", topic: "x", sentiment: "neutral", claim: "x", evidenceIds: ["ev_x"], confidence: { level: "signal", rating: "low", inputs: {}, reasons: [{ driver: "clarity", effect: "lowers", note: "n/a" }] } }];
  expectFail(
    "insufficient with signals field rejected (discriminated union / strict)",
    bad,
  );
}
{
  const bad = clone(noSources);
  // Reason kind "no_sources_found" with non-empty sourceDocuments is inconsistent.
  bad.sourceDocuments = [
    {
      id: "src_x",
      url: "https://example.test/x",
      platform: "blog",
      author: null,
      capturedAt: "2026-05-01T00:00:00Z",
      publishedAt: null,
    },
  ];
  bad.sourceAttributions = [
    {
      id: "att_x",
      sourceDocumentId: "src_x",
      sourceType: "blog",
      url: "https://example.test/x",
      fetchedAt: "2026-05-01T00:00:00Z",
      accessedVia: "direct_fetch",
      trustTier: "low_context",
    },
  ];
  expectFailWithPath(
    "insufficient kind=no_sources_found with non-empty sourceDocuments rejected",
    bad,
    "insufficientDataReason.kind",
  );
}
{
  const bad = clone(unusable);
  bad.sourceCoverage[0].sourceDocumentId = "src_does_not_exist";
  expectFailWithPath(
    "sourceCoverage referencing unknown sourceDocumentId rejected",
    bad,
    "sourceCoverage.0.sourceDocumentId",
  );
}

// ============================================================
//   `unsupported_category` arm — scope refusal
// ============================================================

{
  const bad = clone(refusal);
  bad.requestedCategory = "employment_intelligence";
  expectFailWithPath(
    "unsupported_category with a *supported* requestedCategory rejected",
    bad,
    "requestedCategory",
  );
}
{
  const bad = clone(refusal);
  bad.supportedCategories = ["employment_intelligence"];
  expectFailWithPath(
    "unsupported_category with incomplete supportedCategories rejected",
    bad,
    "supportedCategories",
  );
}
{
  const bad = clone(refusal);
  bad.signals = [];
  expectFail(
    "unsupported_category carrying any evidence-shaped key rejected (strict)",
    bad,
  );
}
{
  const bad = clone(refusal);
  bad.themes = [];
  expectFail(
    "unsupported_category carrying themes key rejected (strict)",
    bad,
  );
}
{
  const bad = clone(refusal);
  bad.evidence = [];
  expectFail(
    "unsupported_category carrying evidence key rejected (strict)",
    bad,
  );
}

// ============================================================
//   Unsupported category enum (subject-level) — kept for parity
// ============================================================

const UNSUPPORTED_CATEGORIES = [
  "banking",
  "healthcare",
  "insurance",
  "schools",
  "consumer_products",
  "legal_eligibility",
  "creditworthiness",
  "medical_suitability",
  "financial_advice",
];
for (const cat of UNSUPPORTED_CATEGORIES) {
  const bad = clone();
  bad.category = cat;
  expectFailWithPath(`sufficient with category="${cat}" rejected`, bad, "category");
}
{
  const bad = clone();
  delete bad.category;
  expectFailWithPath("sufficient missing category rejected", bad, "category");
}

// ============================================================
//   Positive coverage — unknown rating on insufficient parses
// ============================================================

{
  const ok = clone(insufficient);
  ok.confidence.rating = "unknown";
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  insufficient with confidence.rating=unknown should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    insufficient with confidence.rating=unknown");
  }
}

// ============================================================
//   Evidence model checks (sufficient arm) — preserved from prior coverage
// ============================================================

{
  const bad = clone();
  bad.signals[0].confidence.reasons = [];
  expectFailWithPath("Signal confidence.reasons cannot be empty", bad, "signals.0.confidence.reasons");
}
{
  const bad = clone();
  bad.themes[0].confidence.reasons = [];
  expectFailWithPath("Theme confidence.reasons cannot be empty", bad, "themes.0.confidence.reasons");
}
{
  const bad = clone();
  bad.confidence.reasons = [];
  expectFailWithPath("Result confidence.reasons cannot be empty", bad, "confidence.reasons");
}
{
  const bad = clone();
  bad.signals[0].confidence.rating = "unknown";
  expectFailWithPath("Signal confidence.rating=unknown rejected", bad, "signals.0.confidence.rating");
}
{
  const bad = clone();
  bad.themes[0].confidence.rating = "unknown";
  expectFailWithPath("Theme confidence.rating=unknown rejected", bad, "themes.0.confidence.rating");
}
{
  const bad = clone();
  bad.signals[0].evidenceIds = [];
  expectFailWithPath("Signal with empty evidenceIds rejected", bad, "signals.0.evidenceIds");
}
{
  const bad = clone();
  bad.evidence[0].extraction = {
    method: "synthesized",
    extractedAt: "2026-05-20T09:00:00Z",
    extractor: "kuro-extractor@0.1.0",
  };
  delete bad.evidence[0].originalSnippet;
  delete bad.evidence[0].qualityHints;
  expectFailWithPath(
    "Synthesized evidence without originalSnippet or notes rejected",
    bad,
    "evidence.0.originalSnippet",
  );
}
{
  const ok = clone();
  ok.evidence[0].extraction = {
    method: "synthesized",
    extractedAt: "2026-05-20T09:00:00Z",
    extractor: "kuro-extractor@0.1.0",
  };
  ok.evidence[0].originalSnippet = "Management changed three times in my first year and each reorg killed morale.";
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  Synthesized evidence with originalSnippet should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    Synthesized evidence with originalSnippet");
  }
}
{
  const ok = clone();
  ok.evidence[0].extraction = {
    method: "synthesized",
    extractedAt: "2026-05-20T09:00:00Z",
    extractor: "kuro-extractor@0.1.0",
  };
  delete ok.evidence[0].originalSnippet;
  ok.evidence[0].qualityHints = { notes: "Paraphrased from a paywalled transcript; verbatim text not retrievable." };
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  Synthesized evidence with qualityHints.notes should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    Synthesized evidence with qualityHints.notes");
  }
}
{
  const bad = clone();
  const dup = JSON.parse(JSON.stringify(bad.evidence[0]));
  dup.id = "ev_dup_1";
  bad.evidence.push(dup);
  expectFailWithPath(
    "Duplicate evidence (sourceDocumentId, locator) rejected without isDuplicateOf",
    bad,
    "evidence",
  );
}
{
  const ok = clone();
  const dup = JSON.parse(JSON.stringify(ok.evidence[0]));
  dup.id = "ev_dup_ok";
  dup.qualityHints = { ...(dup.qualityHints ?? {}), isDuplicateOf: ok.evidence[0].id };
  ok.evidence.push(dup);
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  Duplicate evidence marked with isDuplicateOf should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    Duplicate evidence marked with isDuplicateOf");
  }
}
{
  const bad = clone();
  bad.evidence[0].qualityHints = { isDuplicateOf: "ev_does_not_exist" };
  expectFailWithPath(
    "qualityHints.isDuplicateOf pointing at unknown evidence rejected",
    bad,
    "evidence.0.qualityHints.isDuplicateOf",
  );
}
{
  const ok = clone();
  ok.evidence[0].locator = { kind: "anchor", value: "p-paragraph-3" };
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  Anchor locator should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    Anchor locator");
  }
}
{
  const bad = clone();
  bad.evidence[0].locator = { kind: "charRange", start: 200, end: 100 };
  expectFailWithPath(
    "charRange locator with end < start rejected",
    bad,
    "evidence.0.locator",
  );
}

// ============================================================
//   SourceAttribution checks — preserved from prior coverage
// ============================================================

{
  const bad = clone();
  bad.sourceAttributions[0].url = "not-a-url";
  expectFailWithPath("Attribution.url rejects non-URL", bad, "sourceAttributions.0.url");
}
{
  const bad = clone();
  bad.sourceAttributions[0].fetchedAt = "2099-01-01T00:00:00Z";
  expectFailWithPath(
    "Attribution.fetchedAt in future rejected",
    bad,
    "sourceAttributions.0.fetchedAt",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].publishedAt = "2030-01-01T00:00:00Z";
  delete bad.sourceAttributions[0].trustRationale;
  expectFailWithPath(
    "Attribution publishedAt > fetchedAt without rationale rejected",
    bad,
    "sourceAttributions.0.publishedAt",
  );
}
{
  const ok = clone();
  ok.sourceAttributions[0].fetchedAt = "2026-05-20T09:00:00Z";
  ok.sourceAttributions[0].publishedAt = "2026-05-20T09:00:01Z";
  ok.sourceAttributions[0].trustRationale = "Source edit timestamp drifted by a few seconds after fetch.";
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  Attribution publishedAt > fetchedAt with rationale should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    Attribution publishedAt > fetchedAt with rationale");
  }
}
{
  const bad = clone();
  bad.sourceAttributions[0].authorHandle = "person@example.com";
  expectFailWithPath(
    "Attribution.authorHandle rejects email shape",
    bad,
    "sourceAttributions.0.authorHandle",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].authorHandle = "Jane Doe";
  expectFailWithPath(
    "Attribution.authorHandle rejects real-name shape",
    bad,
    "sourceAttributions.0.authorHandle",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].sourceType = "not_a_type";
  expectFailWithPath(
    "Attribution.sourceType outside enum rejected",
    bad,
    "sourceAttributions.0.sourceType",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].trustTier = "not_a_tier";
  expectFailWithPath(
    "Attribution.trustTier outside enum rejected",
    bad,
    "sourceAttributions.0.trustTier",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].trustTier = "unknown";
  delete bad.sourceAttributions[0].trustRationale;
  expectFailWithPath(
    'Attribution.trustTier "unknown" without rationale rejected',
    bad,
    "sourceAttributions.0.trustRationale",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].redactions = [
    { field: "authorHandle", category: "email", value: "person@example.com" },
  ];
  expectFailWithPath(
    "RedactionRecord with raw value key rejected",
    bad,
    "sourceAttributions.0.redactions.0",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].sourceDocumentId = "src_does_not_exist";
  expectFailWithPath(
    "Attribution.sourceDocumentId pointing at unknown document rejected",
    bad,
    "sourceAttributions.0.sourceDocumentId",
  );
}
{
  const bad = clone();
  const dup = JSON.parse(JSON.stringify(bad.sourceAttributions[0]));
  dup.id = "att_dup";
  bad.sourceAttributions.push(dup);
  expectFailWithPath(
    "Two attributions for same sourceDocumentId rejected (1:1)",
    bad,
    "sourceAttributions",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].canonicalUrl =
    "https://www.reddit.com/r/jobs/comments/abc123/two_years_at_acme?utm_source=newsletter";
  expectFailWithPath(
    "Attribution.canonicalUrl with utm_source rejected",
    bad,
    "sourceAttributions.0.canonicalUrl",
  );
}
{
  const bad = clone();
  delete bad.sourceAttributions[0].url;
  delete bad.sourceAttributions[0].canonicalUrl;
  delete bad.sourceAttributions[0].accessedVia;
  expectFailWithPath(
    "Attribution with neither url nor accessedVia rejected",
    bad,
    "sourceAttributions.0.url",
  );
}
{
  const ok = clone();
  delete ok.sourceAttributions[0].url;
  delete ok.sourceAttributions[0].canonicalUrl;
  delete ok.sourceAttributions[0].authorHandle;
  ok.sourceAttributions[0].accessedVia = "user_paste";
  ok.sourceAttributions[0].sourceType = "other";
  ok.sourceAttributions[0].trustTier = "low_context";
  const r = KuroResult.safeParse(ok);
  if (!r.success) {
    failures++;
    console.error("FAIL  User-pasted attribution should parse, got:");
    for (const issue of r.error.issues) {
      console.error(`  - ${issue.path.join(".")}: ${issue.message}`);
    }
  } else {
    console.log("OK    User-pasted attribution (no URL, accessedVia=user_paste)");
  }
}
{
  const bad = clone();
  bad.sourceAttributions = bad.sourceAttributions.slice(1);
  expectFailWithPath(
    "sufficient without attribution for every SourceDocument rejected",
    bad,
    "sourceAttributions",
  );
}
{
  const bad = clone();
  const big: Record<string, number> = {};
  for (let i = 0; i < 21; i++) big[`k${i}`] = i;
  bad.sourceAttributions[0].metadata = big;
  expectFailWithPath(
    "Attribution.metadata with >20 keys rejected",
    bad,
    "sourceAttributions.0.metadata",
  );
}
{
  const bad = clone();
  bad.sourceAttributions[0].metadata = { nested: { foo: "bar" } };
  expectFailWithPath(
    "Attribution.metadata with nested object rejected",
    bad,
    "sourceAttributions.0.metadata",
  );
}

// ============================================================
//   Theme.maySuggest must be ThemeClaim, not InferenceClaim
// ============================================================

{
  const bad = clone();
  bad.themes[0].maySuggest[0] = { description: "test", themeIds: ["theme_management"] };
  expectFailWithPath(
    "Theme.maySuggest rejects InferenceClaim-shaped entry (extra themeIds)",
    bad,
    "themes.0.maySuggest",
  );
}

// ============================================================
//   Trust & transparency — user-facing wording lint (issue #8)
// ============================================================

// Every positive fixture's KURO-authored prose must be clean.
for (const name of WORDING_CLEAN_EXAMPLES) {
  const data = load(name);
  const findings = lintKuroResultWording(data as Parameters<typeof lintKuroResultWording>[0]);
  if (findings.length > 0) {
    failures++;
    console.error(`FAIL  ${name}: forbidden user-facing wording found:`);
    for (const f of findings) {
      console.error(`  - ${f.path}: [${f.category}] "${f.match}" (${f.rule})`);
    }
  } else {
    console.log(`OK    ${name} (wording clean)`);
  }
}

// The negative fixture must trip the lint across multiple layers.
{
  const data = load("forbidden-wording.json");
  const findings = lintKuroResultWording(data as Parameters<typeof lintKuroResultWording>[0]);
  const parsed = KuroResult.safeParse(data);
  if (!parsed.success) {
    failures++;
    console.error("FAIL  forbidden-wording.json: expected to parse structurally (lint, not schema, is the gate)");
  } else if (findings.length === 0) {
    failures++;
    console.error("FAIL  forbidden-wording.json: expected wording-lint findings, got none");
  } else {
    const haveVerdict = findings.some((f) => f.category === "verdict");
    const haveDirective = findings.some((f) => f.category === "directive");
    const inSummary = findings.some((f) => f.path === "summary");
    const inFinalKuro = findings.some((f) => f.path === "finalKuro");
    if (haveVerdict && haveDirective && inSummary && inFinalKuro) {
      console.log(
        `OK    forbidden-wording.json (parses, but wording lint flags ${findings.length} issues incl. summary + finalKuro, verdict + directive)`,
      );
    } else {
      failures++;
      console.error(
        "FAIL  forbidden-wording.json: lint hits did not cover the expected spread " +
          `(verdict=${haveVerdict}, directive=${haveDirective}, summary=${inSummary}, finalKuro=${inFinalKuro})`,
      );
    }
  }
}

if (failures > 0) {
  console.error(`\n${failures} check(s) failed.`);
  process.exit(1);
}
console.log("\nAll schema checks passed.");
