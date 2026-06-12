package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.ExtractionMethod;
import com.acltabontabon.kuro.domain.LocatorKind;
import com.acltabontabon.kuro.domain.SourceTrust;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Locator/extraction/quality value objects flattened, matching the DDL.
@Entity
@Table(name = "evidence")
class EvidenceEntity extends BaseEntity {

    String resultId;
    String sourceDocumentId;
    String snippet;
    String originalSnippet;
    LocatorKind locatorKind;
    Integer locatorStart;
    Integer locatorEnd;
    Integer locatorStartLine;
    Integer locatorEndLine;
    String locatorAnchor;
    ExtractionMethod extractionMethod;
    String extractedAt;
    String extractor;
    SourceTrust qualitySourceTrust;
    String qualityIsDuplicateOf;
    String qualityNotes;
}
