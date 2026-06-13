package com.acltabontabon.kuro.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Vendor AI SDKs are confined to the {@code ai} adapter package (issue #17).
 * No other layer may import a provider SDK, so the {@code AiProvider} seam stays
 * the only place that knows which vendor is in use.
 */
@AnalyzeClasses(packages = "com.acltabontabon.kuro", importOptions = ImportOption.DoNotIncludeTests.class)
class VendorIsolationTest {

    @ArchTest
    static final ArchRule vendorSdksConfinedToAiPackage =
            noClasses().that().resideOutsideOfPackage("com.acltabontabon.kuro.ai..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.google..",
                            "com.openai..",
                            "com.anthropic..",
                            "dev.langchain4j..")
                    .allowEmptyShould(true);
}
