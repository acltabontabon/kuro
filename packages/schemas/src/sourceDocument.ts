import { z } from "zod";
import { Id, IsoDateTime, Url } from "./primitives.js";

export const SourceDocument = z.object({
  id: Id,
  url: Url,
  platform: z.string().min(1),
  author: z.string().nullable(),
  capturedAt: IsoDateTime,
  publishedAt: IsoDateTime.nullable(),
  content: z.string().optional(),
  contentHash: z.string().optional(),
  context: z.string().optional(),
});
export type SourceDocument = z.infer<typeof SourceDocument>;
