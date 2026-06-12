package com.acltabontabon.kuro.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the layering api -> application -> {domain, persistence}; persistence
 * -> domain. Rules apply to main code only (tests may depend across layers).
 */
@AnalyzeClasses(packages = "com.acltabontabon.kuro", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerBoundaryTest {

    @ArchTest
    static final ArchRule apiDoesNotDependOnPersistence =
            noClasses().that().resideInAPackage("com.acltabontabon.kuro.api..")
                    .should().dependOnClassesThat().resideInAPackage("com.acltabontabon.kuro.persistence..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotDependOnApi =
            noClasses().that().resideInAPackage("com.acltabontabon.kuro.application..")
                    .should().dependOnClassesThat().resideInAPackage("com.acltabontabon.kuro.api..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceDoesNotDependOnApiOrApplication =
            noClasses().that().resideInAPackage("com.acltabontabon.kuro.persistence..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.acltabontabon.kuro.api..",
                            "com.acltabontabon.kuro.application..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule nothingDependsOnApi =
            noClasses().that().resideOutsideOfPackage("com.acltabontabon.kuro.api..")
                    .should().dependOnClassesThat().resideInAPackage("com.acltabontabon.kuro.api..")
                    .allowEmptyShould(true);
}
