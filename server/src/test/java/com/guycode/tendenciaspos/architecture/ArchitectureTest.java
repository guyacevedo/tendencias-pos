package com.guycode.tendenciaspos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Reglas de la arquitectura hexagonal por módulo (ver CLAUDE.md). */
@AnalyzeClasses(packages = "com.guycode.tendenciaspos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dominioSinFramework = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .allowEmptyShould(true)
            .because("el dominio debe poder probarse sin Spring ni JPA");

    @ArchTest
    static final ArchRule dominioNoUsaAdaptadores = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..application..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modulosSinCiclos = slices().matching("com.guycode.tendenciaspos.(*)..")
            .should()
            .beFreeOfCycles()
            .allowEmptyShould(true);
}
