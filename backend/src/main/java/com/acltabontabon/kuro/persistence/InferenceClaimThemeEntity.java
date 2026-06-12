package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// InferenceClaim.themeIds join rows; ordinal preserves the schema's array order.
@Entity
@Table(name = "inference_claim_theme")
class InferenceClaimThemeEntity extends BaseEntity {

    String inferenceClaimId;
    String themeId;
    int ordinal;
}
