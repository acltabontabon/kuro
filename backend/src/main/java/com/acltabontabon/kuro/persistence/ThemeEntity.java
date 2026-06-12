package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.Sentiment;
import com.acltabontabon.kuro.domain.SubResultConfidenceRating;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "theme")
class ThemeEntity extends BaseEntity {

    String resultId;
    String topic;
    Sentiment sentiment;
    SubResultConfidenceRating confidenceRating;
    Double confidenceSupportScore;
    Integer confidenceInputSourceCount;
    Double confidenceInputSourceDiversity;
    Double confidenceInputSourceFreshness;
    Double confidenceInputSignalConsistency;
    String confidenceReasonsJson;
    String maySuggestJson;
    String mayNotSuggestJson;
    String limitationsJson;
}
