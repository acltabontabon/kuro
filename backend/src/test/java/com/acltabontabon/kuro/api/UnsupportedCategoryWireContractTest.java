package com.acltabontabon.kuro.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.KuroResult;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.persistence.FixtureLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Proves the API serializes to the @kuro/schemas wire shape. {@code
 * unsupported_category} is the only variant fully produced and returned this
 * milestone, so it is the one we assert byte-for-byte against the checked-in
 * example. For evidence-bearing variants we only prove the internal confidence
 * diagnostics never leak — a full diff is deliberately NOT done because the
 * example fixtures include those internal fields (and a confidence {@code level}
 * the domain does not yet model).
 */
class UnsupportedCategoryWireContractTest {

    private static final Path UNSUPPORTED = Path.of("../packages/schemas/examples/unsupported-category.json");
    private static final Path EMPLOYMENT = Path.of("../packages/schemas/examples/result.employment.json");

    @Test
    void refusalSerializesToTheCheckedInExample() throws IOException {
        var result = new KuroResult.UnsupportedCategory(
                "result_healthcare_refusal_2026_06_11",
                new Subject("subject_healthcare_query", SubjectKind.OTHER, "Cedar Valley Family Clinic",
                        "User requested community-perspective intelligence about a healthcare provider."),
                OffsetDateTime.parse("2026-06-11T16:00:00Z"),
                null,
                "healthcare",
                List.of(DecisionCategory.EMPLOYMENT_INTELLIGENCE, DecisionCategory.RENTAL_INTELLIGENCE),
                "KURO does not evaluate healthcare providers. KURO's MVP scope is limited to "
                        + "employment_intelligence and rental_intelligence. This is a scope decision, not an "
                        + "assessment of the clinic: KURO did not gather or interpret any community feedback for "
                        + "this subject, and nothing here should be read as a view on the clinic's quality.");

        JsonNode actual = KuroApiJson.mapper().readTree(KuroApiJson.mapper().writeValueAsString(result));
        JsonNode expected = KuroApiJson.mapper().readTree(Files.readString(UNSUPPORTED));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void evidenceVariantsNeverLeakInternalConfidenceFields() throws IOException {
        KuroResult sufficient = FixtureLoader.load(EMPLOYMENT);

        String json = KuroApiJson.mapper().writeValueAsString(sufficient);

        assertThat(json).doesNotContain("supportScore", "inputs");
    }
}
