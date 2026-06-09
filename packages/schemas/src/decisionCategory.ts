import { z } from "zod";

/**
 * The supported scope KURO operates in for a given Result.
 *
 * MVP is intentionally narrow: only employment and rental intelligence.
 * Category controls scope and interpretation, not truth, and is never
 * a user decision outcome. See docs/DECISION_CATEGORIES.md.
 */
export const DecisionCategory = z.enum([
  "employment_intelligence",
  "rental_intelligence",
]);
export type DecisionCategory = z.infer<typeof DecisionCategory>;
