package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.DataSufficiency;
import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.InsufficientDataReasonKind;
import com.acltabontabon.kuro.domain.ResultConfidenceRating;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.type.NumericBooleanConverter;

/**
 * The wide kuro_result table: all four KuroResult variants in one row,
 * variant-conditional columns nullable (governed app-layer; see
 * V1__schema.sql rule 1). model_id/prompt_version are intentionally
 * unmapped until #16.
 */
@Entity
@Table(name = "kuro_result")
class KuroResultEntity extends BaseEntity {

    String requestId;
    int version;
    @Convert(converter = NumericBooleanConverter.class)
    boolean isCurrent;
    String subjectId;
    DataSufficiency dataSufficiency;
    String generatedAt;
    DecisionCategory category;
    String summary;
    String finalKuro;
    String limitationsJson;
    ResultConfidenceRating confidenceRating;
    Double confidenceSupportScore;
    Integer confidenceInputSourceCount;
    Double confidenceInputSourceDiversity;
    Double confidenceInputSourceFreshness;
    Double confidenceInputSignalConsistency;
    Double confidenceInputThemeSupportAggregate;
    Double confidenceInputTopicBreadth;
    String confidenceReasonsJson;
    String inferenceCommunitySentimentSummary;
    String inferenceLimitationsJson;
    String sourceSummaryJson;
    String evidenceGapsJson;
    InsufficientDataReasonKind insufficientReasonKind;
    String insufficientReasonExplanation;
    String suggestedNextSourcesJson;
    String requestedCategory;
    String supportedCategoriesJson;
    String refusalMessage;
}
