package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.AccessedVia;
import com.acltabontabon.kuro.domain.ConfidenceDriver;
import com.acltabontabon.kuro.domain.ConfidenceEffect;
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
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * The shared mapper for *_json columns. Domain enums are written as their
 * schema wire strings so stored JSON matches @kuro/schemas verbatim.
 */
final class KuroJson {

    private static final JsonMapper MAPPER = createMapper();

    private KuroJson() {
    }

    static JsonMapper mapper() {
        return MAPPER;
    }

    static String write(Object value) {
        return value == null ? null : MAPPER.writeValueAsString(value);
    }

    static <T> T read(String json, Class<T> type) {
        return json == null ? null : MAPPER.readValue(json, type);
    }

    static <T> T read(String json, TypeReference<T> type) {
        return json == null ? null : MAPPER.readValue(json, type);
    }

    private static JsonMapper createMapper() {
        SimpleModule module = new SimpleModule("kuro-wire-enums");
        register(module, AccessedVia.class);
        register(module, ConfidenceDriver.class);
        register(module, ConfidenceEffect.class);
        register(module, DataSufficiency.class);
        register(module, DecisionCategory.class);
        register(module, ExtractionMethod.class);
        register(module, InsufficientDataReasonKind.class);
        register(module, LocatorKind.class);
        register(module, RedactionCategory.class);
        register(module, RequestStatus.class);
        register(module, ResultConfidenceRating.class);
        register(module, Sentiment.class);
        register(module, SourceCoverageAssessment.class);
        register(module, SourceTrust.class);
        register(module, SourceType.class);
        register(module, SubResultConfidenceRating.class);
        register(module, SubjectKind.class);
        register(module, TrustTier.class);
        return JsonMapper.builder().addModule(module).build();
    }

    private static <E extends Enum<E> & WireEnum> void register(SimpleModule module, Class<E> type) {
        module.addSerializer(type, new ValueSerializer<E>() {
            @Override
            public void serialize(E value, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeString(value.wire());
            }
        });
        module.addDeserializer(type, new ValueDeserializer<E>() {
            @Override
            public E deserialize(JsonParser p, DeserializationContext ctxt) {
                return WireEnum.fromWire(type, p.getString());
            }
        });
    }
}
