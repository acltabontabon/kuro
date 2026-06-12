package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Signal.evidenceIds join rows; ordinal preserves the schema's array order.
@Entity
@Table(name = "signal_evidence")
class SignalEvidenceEntity extends BaseEntity {

    String signalId;
    String evidenceId;
    int ordinal;
}
