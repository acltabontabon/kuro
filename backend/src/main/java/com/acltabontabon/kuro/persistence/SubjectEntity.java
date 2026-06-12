package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.SubjectKind;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "subject")
class SubjectEntity extends BaseEntity {

    SubjectKind kind;
    String displayName;
    String description;
}
