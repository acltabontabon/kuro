import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { KuroResult } from "../src/index.js";

const here = dirname(fileURLToPath(import.meta.url));

const POSITIVE_EXAMPLES = ["employer-acme.json", "rental-123-main.json"];

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

// Negative tests: each one breaks exactly one traceability invariant.
const base = load("employer-acme.json") as Record<string, unknown>;

function clone(): any {
  return JSON.parse(JSON.stringify(base));
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
  bad.sourceDocuments = [];
  expectFailWithPath("sourceDocuments must be non-empty", bad, "sourceDocuments");
}

{
  const bad = clone();
  bad.inference.maySuggest[0].themeIds = ["theme_does_not_exist"];
  expectFailWithPath("Inference.maySuggest -> missing themeId", bad, "maySuggest");
}

if (failures > 0) {
  console.error(`\n${failures} check(s) failed.`);
  process.exit(1);
}
console.log("\nAll schema checks passed.");
