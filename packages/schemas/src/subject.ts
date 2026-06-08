import { z } from "zod";
import { Id } from "./primitives.js";

export const SubjectKind = z.enum([
  "employer",
  "rental",
  "product",
  "service",
  "location",
  "role",
  "other",
]);
export type SubjectKind = z.infer<typeof SubjectKind>;

export const Subject = z.object({
  id: Id,
  kind: SubjectKind,
  displayName: z.string().min(1),
  description: z.string().optional(),
});
export type Subject = z.infer<typeof Subject>;
