package com.acltabontabon.kuro.api;

import com.acltabontabon.kuro.domain.DataSufficiency;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.ResultConfidence;
import com.acltabontabon.kuro.domain.SignalConfidence;
import com.acltabontabon.kuro.domain.ThemeConfidence;
import com.acltabontabon.kuro.domain.WireEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * The API response mapper: serializes domain objects to the @kuro/schemas wire
 * shape. Every {@link WireEnum} is written as its schema string (one generic
 * serializer, no per-enum list), timestamps as IsoDateTime with offset, and the
 * internal confidence diagnostics ({@code supportScore}/{@code inputs}) are
 * dropped via mix-ins so they never reach a client. Null optionals are omitted.
 *
 * <p>Only the {@code unsupported_category} variant is actually returned to a
 * client this milestone (it has no confidence/evidence chain); the mix-ins and
 * the evidence-variant serializers are not yet on the wire contract.
 */
final class KuroApiJson {

    private static final JsonMapper MAPPER = createMapper();

    private KuroApiJson() {
    }

    static JsonMapper mapper() {
        return MAPPER;
    }

    private static JsonMapper createMapper() {
        var module = new SimpleModule("kuro-api-wire");
        module.addSerializer(WireEnum.class, new ValueSerializer<WireEnum>() {
            @Override
            public void serialize(WireEnum value, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeString(value.wire());
            }
        });
        module.addSerializer(OffsetDateTime.class, new ValueSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
        return JsonMapper.builder()
                .addModule(module)
                .addMixIn(KuroResult.class, KuroResultMixin.class)
                .addMixIn(ResultConfidence.class, ConfidenceMixin.class)
                .addMixIn(SignalConfidence.class, ConfidenceMixin.class)
                .addMixIn(ThemeConfidence.class, ConfidenceMixin.class)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    /** {@code dataSufficiency} is an interface method, not a record component, so
     * Jackson skips it without this — but it is the union discriminator on the wire. */
    private interface KuroResultMixin {
        @JsonProperty("dataSufficiency")
        DataSufficiency dataSufficiency();
    }

    @JsonIgnoreProperties({"supportScore", "inputs"})
    private interface ConfidenceMixin {
    }
}
