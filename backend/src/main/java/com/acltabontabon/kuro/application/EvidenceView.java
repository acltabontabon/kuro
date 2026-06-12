package com.acltabontabon.kuro.application;

import com.acltabontabon.kuro.domain.Evidence;
import com.acltabontabon.kuro.domain.SourceAttribution;

/**
 * One entry in the evidence-explorer response (#13): a piece of {@link Evidence}
 * paired with its redacted {@link SourceAttribution} ({@code source} is null if
 * the document has no attribution). Carries no raw source author/content.
 */
public record EvidenceView(Evidence evidence, SourceAttribution source) {
}
