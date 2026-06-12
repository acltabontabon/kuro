package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.WireEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per claim across KuroInference's five arrays; kind maps the
 * schema's object keys to the DDL's snake_case CHECK list. Kind is a
 * storage-only concept (the domain keeps five separate lists), so the enum
 * lives here, not in domain.
 */
@Entity
@Table(name = "inference_claim")
class InferenceClaimEntity extends BaseEntity {

    String resultId;
    Kind kind;
    String description;
    int ordinal;

    enum Kind implements WireEnum {
        PATTERNS("patterns"),
        CONSENSUS("consensus"),
        DISAGREEMENTS("disagreements"),
        MAY_SUGGEST("may_suggest"),
        MAY_NOT_SUGGEST("may_not_suggest");

        private final String wire;

        Kind(String wire) {
            this.wire = wire;
        }

        @Override
        public String wire() {
            return wire;
        }
    }
}
