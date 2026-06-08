import { z } from "zod";

export const Id = z.string().min(1);
export type Id = z.infer<typeof Id>;

export const Url = z.string().url();
export type Url = z.infer<typeof Url>;

export const IsoDateTime = z.string().datetime({ offset: true });
export type IsoDateTime = z.infer<typeof IsoDateTime>;

export const SupportScore = z.number().min(0).max(1);
export type SupportScore = z.infer<typeof SupportScore>;

export const ConfidenceRating = z.enum(["low", "medium", "high", "unknown"]);
export type ConfidenceRating = z.infer<typeof ConfidenceRating>;
