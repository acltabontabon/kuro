import type { KuroResult } from "./result.js";

/**
 * Wording lint for KURO-authored, user-facing prose.
 *
 * KURO's trust posture (see docs/TRUST_AND_TRANSPARENCY.md) forbids two
 * families of phrasing in any text KURO writes about a Subject:
 *
 *   - verdict-like claims — labelling a Subject good/bad/safe/unsafe,
 *     asserting community sentiment as proven fact, or treating confidence
 *     as probability of truth.
 *   - directive language — telling the user what to do (accept, avoid,
 *     resign, report, rent, etc.).
 *
 * This lint is deliberately HIGH-PRECISION, not exhaustive. KURO's own
 * cautious style routinely *names* the forbidden actions in order to
 * disclaim them ("…is not a recommendation to accept or decline an
 * offer", "Does not recommend whether to…"). A broad keyword ban would
 * flag that legitimate meta-framing. The patterns below therefore target
 * only unambiguous imperative/verdict surface forms that cautious framing
 * never produces. Rules that cannot be caught precisely here are enforced
 * as documented prompt/UI/product constraints in TRUST_AND_TRANSPARENCY.md,
 * not by this function.
 *
 * The lint runs only over KURO-authored interpretation strings. It never
 * inspects quoted Evidence (`snippet`, `originalSnippet`) or source-derived
 * metadata (titles, author handles, raw content), which may legitimately
 * contain any wording at all.
 */

export type WordingCategory = "verdict" | "directive";

export interface ForbiddenPhraseRule {
  category: WordingCategory;
  pattern: RegExp;
  label: string;
}

/**
 * Curated forbidden surface forms. Each pattern is anchored on word
 * boundaries and avoids the inflections KURO uses for cautious disclaimers
 * (e.g. it bans the verdict label "recommended"/"recommends" but not the
 * noun "recommendation" or the disclaiming verb phrase "does not recommend").
 */
export const FORBIDDEN_PHRASE_RULES: ForbiddenPhraseRule[] = [
  // --- Directive: telling the user what to do -----------------------------
  { category: "directive", pattern: /\byou\s+should\b/i, label: "you should" },
  { category: "directive", pattern: /\byou\s+must\b/i, label: "you must" },
  { category: "directive", pattern: /\byou\s+ought\s+to\b/i, label: "you ought to" },
  { category: "directive", pattern: /\bdo\s+not\s+work\s+(there|here|for)\b/i, label: "do not work there/here/for" },
  { category: "directive", pattern: /\bdon'?t\s+(rent|apply|accept|take)\b/i, label: "don't rent/apply/accept/take" },
  { category: "directive", pattern: /\bavoid\s+this\b/i, label: "avoid this <subject>" },
  { category: "directive", pattern: /\bsteer\s+clear\s+of\b/i, label: "steer clear of" },
  { category: "directive", pattern: /\bstay\s+away\s+from\b/i, label: "stay away from" },

  // --- Verdict: labelling the Subject or asserting truth -------------------
  { category: "verdict", pattern: /\btoxic\s+workplace\b/i, label: "toxic workplace" },
  { category: "verdict", pattern: /\bis\s+(an?\s+)?unsafe\b/i, label: "is unsafe" },
  { category: "verdict", pattern: /\bis\s+(a\s+)?dangerous\b/i, label: "is dangerous" },
  { category: "verdict", pattern: /\bis\s+(a\s+)?(bad|terrible|awful)\b/i, label: "is bad/terrible/awful" },
  { category: "verdict", pattern: /\bis\s+abusive\b/i, label: "is abusive" },
  { category: "verdict", pattern: /\b(good|bad|great|terrible)\s+(employer|landlord|company|workplace|property|building)\b/i, label: "<verdict> employer/landlord/property" },
  { category: "verdict", pattern: /\bthis\s+proves\b/i, label: "this proves" },
  { category: "verdict", pattern: /\bproves\s+that\b/i, label: "proves that" },
  { category: "verdict", pattern: /\b(means|proves|confirms)\s+(this|it)\s+is\s+true\b/i, label: "means/proves/confirms this is true" },
  { category: "verdict", pattern: /\bhigh\s+confidence\s+means\b/i, label: "high confidence means …" },
  { category: "verdict", pattern: /\bis\s+probably\b/i, label: "is probably" },
  // Standalone verdict labels (UI chips / one-word summaries). Inflection-
  // specific so cautious nouns ("recommendation", "approval") are not caught.
  { category: "verdict", pattern: /\brecommended\b/i, label: "Recommended (verdict label)" },
  { category: "verdict", pattern: /\brecommends\b/i, label: "recommends (verdict label)" },
  { category: "verdict", pattern: /\bapproved\b/i, label: "Approved (verdict label)" },
  { category: "verdict", pattern: /\brejected\b/i, label: "Rejected (verdict label)" },
];

export interface WordingFinding {
  /** Dotted path to the offending string within the KuroResult. */
  path: string;
  /** The forbidden phrase that matched, as it appears in the text. */
  match: string;
  category: WordingCategory;
  /** Human-readable rule label. */
  rule: string;
}

/** Scan a single string against the forbidden-phrase rules. */
export function findForbiddenWording(text: string): Omit<WordingFinding, "path">[] {
  const findings: Omit<WordingFinding, "path">[] = [];
  for (const rule of FORBIDDEN_PHRASE_RULES) {
    const m = rule.pattern.exec(text);
    if (m) {
      findings.push({ match: m[0], category: rule.category, rule: rule.label });
    }
  }
  return findings;
}

type Walkable = Record<string, unknown> | undefined | null;

function pushStrings(
  acc: { path: string; text: string }[],
  path: string,
  value: unknown,
): void {
  if (typeof value === "string" && value.length > 0) {
    acc.push({ path, text: value });
  }
}

/**
 * Collect every KURO-authored interpretation string from a KuroResult,
 * paired with a dotted path. Quoted Evidence and source-derived metadata are
 * intentionally excluded — only text KURO itself wrote about the Subject.
 */
export function collectUserFacingStrings(
  result: KuroResult,
): { path: string; text: string }[] {
  const acc: { path: string; text: string }[] = [];
  const r = result as unknown as Record<string, unknown>;

  pushStrings(acc, "summary", r.summary);
  pushStrings(acc, "finalKuro", r.finalKuro);
  pushStrings(acc, "refusalMessage", r.refusalMessage);

  const limitations = r.limitations;
  if (Array.isArray(limitations)) {
    limitations.forEach((l, i) => pushStrings(acc, `limitations.${i}`, l));
  }

  const reason = r.insufficientDataReason as Walkable;
  if (reason) pushStrings(acc, "insufficientDataReason.explanation", reason.explanation);

  const suggested = r.suggestedNextSources;
  if (Array.isArray(suggested)) {
    suggested.forEach((s, i) =>
      pushStrings(acc, `suggestedNextSources.${i}.rationale`, (s as Walkable)?.rationale),
    );
  }

  const gaps = r.evidenceGaps;
  if (Array.isArray(gaps)) {
    gaps.forEach((g, i) => {
      pushStrings(acc, `evidenceGaps.${i}.topic`, (g as Walkable)?.topic);
      pushStrings(acc, `evidenceGaps.${i}.note`, (g as Walkable)?.note);
    });
  }

  const signals = r.signals;
  if (Array.isArray(signals)) {
    signals.forEach((s, i) => pushStrings(acc, `signals.${i}.claim`, (s as Walkable)?.claim));
  }

  const themes = r.themes;
  if (Array.isArray(themes)) {
    themes.forEach((t, i) => {
      const theme = t as Walkable;
      (["maySuggest", "mayNotSuggest"] as const).forEach((k) => {
        const arr = theme?.[k];
        if (Array.isArray(arr)) {
          arr.forEach((c, j) =>
            pushStrings(acc, `themes.${i}.${k}.${j}.description`, (c as Walkable)?.description),
          );
        }
      });
      const lims = theme?.limitations;
      if (Array.isArray(lims)) {
        lims.forEach((l, j) => pushStrings(acc, `themes.${i}.limitations.${j}`, l));
      }
    });
  }

  const inference = r.inference as Walkable;
  if (inference) {
    pushStrings(acc, "inference.communitySentimentSummary", inference.communitySentimentSummary);
    (["patterns", "consensus", "disagreements", "maySuggest", "mayNotSuggest"] as const).forEach(
      (k) => {
        const arr = inference[k];
        if (Array.isArray(arr)) {
          arr.forEach((c, i) =>
            pushStrings(acc, `inference.${k}.${i}.description`, (c as Walkable)?.description),
          );
        }
      },
    );
    const lims = inference.limitations;
    if (Array.isArray(lims)) {
      lims.forEach((l, i) => pushStrings(acc, `inference.limitations.${i}`, l));
    }
  }

  return acc;
}

/**
 * Lint a whole KuroResult for forbidden user-facing wording. Returns one
 * finding per (string, rule) match. An empty array means the result's
 * KURO-authored prose is clean against the curated rules — it does NOT
 * certify compliance with every principle, only with the structurally
 * checkable subset. See docs/TRUST_AND_TRANSPARENCY.md.
 */
export function lintKuroResultWording(result: KuroResult): WordingFinding[] {
  const findings: WordingFinding[] = [];
  for (const { path, text } of collectUserFacingStrings(result)) {
    for (const f of findForbiddenWording(text)) {
      findings.push({ path, ...f });
    }
  }
  return findings;
}
