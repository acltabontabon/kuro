package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.RedactionCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "redaction")
class RedactionEntity extends BaseEntity {

    String sourceAttributionId;
    String field;
    RedactionCategory category;
    String reason;
}
