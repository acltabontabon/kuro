package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.DataSufficiency;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Locator;
import com.acltabontabon.kuro.domain.WireEnum;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Reads a packages/schemas example JSON into the domain model: the shared
 * KuroJson mapper plus test-only handling for the two discriminated unions
 * (Locator by "kind", KuroResult by "dataSufficiency") and JSON-only
 * literals like confidence "level", which have no domain counterpart.
 */
final class FixtureLoader {

    private static final JsonMapper MAPPER = KuroJson.mapper().rebuild()
            .addModule(new SimpleModule("kuro-test-unions")
                    .addDeserializer(Locator.class, new LocatorDeserializer()))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private record Probe(String dataSufficiency) {
    }

    private FixtureLoader() {
    }

    static KuroResult load(Path path) throws IOException {
        String json = Files.readString(path);
        var status = WireEnum.fromWire(DataSufficiency.class, MAPPER.readValue(json, Probe.class).dataSufficiency());
        Class<? extends KuroResult> type = switch (status) {
            case SUFFICIENT -> KuroResult.Sufficient.class;
            case PARTIAL -> KuroResult.Partial.class;
            case INSUFFICIENT -> KuroResult.Insufficient.class;
            case UNSUPPORTED_CATEGORY -> KuroResult.UnsupportedCategory.class;
        };
        return MAPPER.readValue(json, type);
    }

    private static final class LocatorDeserializer extends ValueDeserializer<Locator> {

        @Override
        public Locator deserialize(JsonParser p, DeserializationContext ctxt) {
            @SuppressWarnings("unchecked")
            Map<String, Object> node = ctxt.readValue(p, Map.class);
            return switch ((String) node.get("kind")) {
                case "charRange" -> new Locator.CharRange(intOf(node, "start"), intOf(node, "end"));
                case "lineRange" -> new Locator.LineRange(intOf(node, "startLine"), intOf(node, "endLine"));
                case "anchor" -> new Locator.Anchor((String) node.get("value"));
                default -> throw new IllegalArgumentException("Unknown locator kind: " + node.get("kind"));
            };
        }

        private static int intOf(Map<String, Object> node, String key) {
            return ((Number) node.get(key)).intValue();
        }
    }
}
