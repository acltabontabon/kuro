package com.acltabontabon.kuro.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Armed before any domain types exist: {@code domain} must stay free of
 * frameworks and must not depend on adapter packages.
 */
@AnalyzeClasses(packages = "com.acltabontabon.kuro", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    @ArchTest
    static final ArchRule domainHasNoFrameworkOrAdapterDependencies =
            noClasses().that().resideInAPackage("com.acltabontabon.kuro.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "org.flywaydb..",
                            "tools.jackson..",
                            "com.fasterxml.jackson..",
                            "com.acltabontabon.kuro.api..",
                            "com.acltabontabon.kuro.persistence..",
                            "com.acltabontabon.kuro.ai..",
                            "com.acltabontabon.kuro.extraction..")
                    .allowEmptyShould(true);
}
