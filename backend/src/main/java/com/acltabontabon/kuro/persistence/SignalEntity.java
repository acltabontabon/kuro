package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.Sentiment;
import com.acltabontabon.kuro.domain.SubResultConfidenceRating;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "signal")
class SignalEntity extends BaseEntity {

    String resultId;
    String topic;
    Sentiment sentiment;
    String claim;
    SubResultConfidenceRating confidenceRating;
    Double confidenceSupportScore;
    Integer confidenceInputSourceCount;
    Double confidenceInputSourceDiversity;
    Double confidenceInputSourceFreshness;
    Double confidenceInputSignalConsistency;
    Double confidenceInputClarity;
    Double confidenceInputLanguageAmbiguity;
    Double confidenceInputDirectnessOfSupport;
    String confidenceReasonsJson;
}
