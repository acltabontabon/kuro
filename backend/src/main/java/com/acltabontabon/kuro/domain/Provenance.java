package com.acltabontabon.kuro.domain;

import java.util.List;

/**
 * The full, reconstructable provenance of one result version (issue #16): the
 * answer to "why does this KURO say what it says". The structural chain —
 * {@link Signal} → {@link Evidence} → source → {@link SourceAttribution} — plus
 * the {@link AiRun} records identifying the AI runs that produced it.
 *
 * <p>{@code attributions} are the redacted attributions for the evidence's
 * source documents (never raw source author/content), matching the PII posture
 * of the evidence-explorer surface. {@code signals} carry their evidenceIds, so
 * each signal walks to the evidence and on to the sources it rests on.
 */
public record Provenance(
        String resultId,
        int version,
        List<Signal> signals,
        List<Evidence> evidence,
        List<SourceAttribution> attributions,
        List<AiRun> aiRuns) {
}
