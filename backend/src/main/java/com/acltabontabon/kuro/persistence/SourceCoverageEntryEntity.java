package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.SourceCoverageAssessment;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_coverage_entry")
class SourceCoverageEntryEntity extends BaseEntity {

    String resultId;
    String sourceDocumentId;
    SourceCoverageAssessment assessment;
    String note;
}
