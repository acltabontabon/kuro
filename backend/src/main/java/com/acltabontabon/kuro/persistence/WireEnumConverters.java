package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.AccessedVia;
import com.acltabontabon.kuro.domain.DataSufficiency;
import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.ExtractionMethod;
import com.acltabontabon.kuro.domain.InsufficientDataReasonKind;
import com.acltabontabon.kuro.domain.LocatorKind;
import com.acltabontabon.kuro.domain.RedactionCategory;
import com.acltabontabon.kuro.domain.RequestStatus;
import com.acltabontabon.kuro.domain.ResultConfidenceRating;
import com.acltabontabon.kuro.domain.Sentiment;
import com.acltabontabon.kuro.domain.SourceCoverageAssessment;
import com.acltabontabon.kuro.domain.SourceTrust;
import com.acltabontabon.kuro.domain.SourceType;
import com.acltabontabon.kuro.domain.SubResultConfidenceRating;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.domain.TrustTier;
import com.acltabontabon.kuro.domain.WireEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Auto-applied converters storing every domain enum as its schema wire
 * string, matching the DDL CHECK lists exactly. If converter discovery ever
 * breaks, Hibernate falls back to ORDINAL mapping and ddl-auto=validate
 * fails at boot — misconfiguration cannot pass silently.
 */
final class WireEnumConverters {

    private WireEnumConverters() {
    }

    abstract static class Base<E extends Enum<E> & WireEnum> implements AttributeConverter<E, String> {

        private final Class<E> type;

        Base(Class<E> type) {
            this.type = type;
        }

        @Override
        public String convertToDatabaseColumn(E attribute) {
            return attribute == null ? null : attribute.wire();
        }

        @Override
        public E convertToEntityAttribute(String dbData) {
            return dbData == null ? null : WireEnum.fromWire(type, dbData);
        }
    }

    @Converter(autoApply = true)
    static class SubjectKindConverter extends Base<SubjectKind> {
        SubjectKindConverter() {
            super(SubjectKind.class);
        }
    }

    @Converter(autoApply = true)
    static class RequestStatusConverter extends Base<RequestStatus> {
        RequestStatusConverter() {
            super(RequestStatus.class);
        }
    }

    @Converter(autoApply = true)
    static class DataSufficiencyConverter extends Base<DataSufficiency> {
        DataSufficiencyConverter() {
            super(DataSufficiency.class);
        }
    }

    @Converter(autoApply = true)
    static class DecisionCategoryConverter extends Base<DecisionCategory> {
        DecisionCategoryConverter() {
            super(DecisionCategory.class);
        }
    }

    @Converter(autoApply = true)
    static class ResultConfidenceRatingConverter extends Base<ResultConfidenceRating> {
        ResultConfidenceRatingConverter() {
            super(ResultConfidenceRating.class);
        }
    }

    @Converter(autoApply = true)
    static class SubResultConfidenceRatingConverter extends Base<SubResultConfidenceRating> {
        SubResultConfidenceRatingConverter() {
            super(SubResultConfidenceRating.class);
        }
    }

    @Converter(autoApply = true)
    static class InsufficientDataReasonKindConverter extends Base<InsufficientDataReasonKind> {
        InsufficientDataReasonKindConverter() {
            super(InsufficientDataReasonKind.class);
        }
    }

    @Converter(autoApply = true)
    static class SourceTypeConverter extends Base<SourceType> {
        SourceTypeConverter() {
            super(SourceType.class);
        }
    }

    @Converter(autoApply = true)
    static class AccessedViaConverter extends Base<AccessedVia> {
        AccessedViaConverter() {
            super(AccessedVia.class);
        }
    }

    @Converter(autoApply = true)
    static class TrustTierConverter extends Base<TrustTier> {
        TrustTierConverter() {
            super(TrustTier.class);
        }
    }

    @Converter(autoApply = true)
    static class RedactionCategoryConverter extends Base<RedactionCategory> {
        RedactionCategoryConverter() {
            super(RedactionCategory.class);
        }
    }

    @Converter(autoApply = true)
    static class LocatorKindConverter extends Base<LocatorKind> {
        LocatorKindConverter() {
            super(LocatorKind.class);
        }
    }

    @Converter(autoApply = true)
    static class ExtractionMethodConverter extends Base<ExtractionMethod> {
        ExtractionMethodConverter() {
            super(ExtractionMethod.class);
        }
    }

    @Converter(autoApply = true)
    static class SourceTrustConverter extends Base<SourceTrust> {
        SourceTrustConverter() {
            super(SourceTrust.class);
        }
    }

    @Converter(autoApply = true)
    static class SentimentConverter extends Base<Sentiment> {
        SentimentConverter() {
            super(Sentiment.class);
        }
    }

    @Converter(autoApply = true)
    static class SourceCoverageAssessmentConverter extends Base<SourceCoverageAssessment> {
        SourceCoverageAssessmentConverter() {
            super(SourceCoverageAssessment.class);
        }
    }

    @Converter(autoApply = true)
    static class InferenceClaimKindConverter extends Base<InferenceClaimEntity.Kind> {
        InferenceClaimKindConverter() {
            super(InferenceClaimEntity.Kind.class);
        }
    }
}
