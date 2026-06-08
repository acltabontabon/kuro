import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { KuroResult } from "../src/index.js";

const here = dirname(fileURLToPath(import.meta.url));

const POSITIVE_EXAMPLES = [
  "employer-acme.json",
  "rental-123-main.json",
  "insufficient-data.json",
];

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

for (const name of POSITIVE_EXAMPLES) {
  expectOk(name);
}

const base = load("employer-acme.json") as Record<string, unknown>;
const insufficient = load("insufficient-data.json") as Record<string, unknown>;

function clone(src: Record<string, unknown> = base): any {
  return JSON.parse(JSON.stringify(src));
}

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

// outcome === "ok" must have non-empty themes/signals/evidence/sourceDocuments
{
  const bad = clone();
  bad.themes = [];
  expectFailWithPath("outcome=ok with empty themes rejected", bad, "themes");
}
{
  const bad = clone();
  bad.sourceDocuments = [];
  expectFailWithPath("outcome=ok with empty sourceDocuments rejected", bad, "sourceDocuments");
}

// outcome === "insufficient_data" must have low/unknown confidence and non-empty limitations
{
  const bad = clone(insufficient);
  bad.confidence.rating = "high";
  expectFailWithPath(
    "outcome=insufficient_data with confidence.rating=high rejected",
    bad,
    "confidence.rating",
  );
}
{
  const bad = clone(insufficient);
  bad.inference.limitations = [];
  expectFailWithPath(
    "outcome=insufficient_data with empty inference.limitations rejected",
    bad,
    "inference.limitations",
  );
}

// Theme-level may/mayNotSuggest validates as ThemeClaim — extra `themeIds` rejected by .strict()
{
  const bad = clone();
  bad.themes[0].maySuggest[0] = { description: "test", themeIds: ["theme_management"] };
  expectFailWithPath(
    "Theme.maySuggest rejects InferenceClaim-shaped entry (extra themeIds)",
    bad,
    "themes.0.maySuggest",
  );
}

if (failures > 0) {
  console.error(`\n${failures} check(s) failed.`);
  process.exit(1);
}
console.log("\nAll schema checks passed.");
