package fr.laucoin.registry.backend.test

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@AnalyzeClasses(
    packages = ["fr.laucoin.registry.backend"],
    importOptions = [DoNotIncludeTests::class],
)
class HexagonalArchitectureTest {
    companion object {
        private const val BACKEND = "fr.laucoin.registry.backend"
        private const val CONFIG = "$BACKEND.config"
        private const val DOMAIN = "$BACKEND.domain"
        private const val INFRASTRUCTURE = "$BACKEND.infrastructure"
    }

    @ArchTest
    fun `Infrastructure should not depend on config`(classes: JavaClasses) = noClasses()
        .that()
        .resideInAPackage("$INFRASTRUCTURE..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("$CONFIG..")
        .check(classes)

    @ArchTest
    fun `Infrastructure internal should not depend on infrastructure external`(classes: JavaClasses) = noClasses()
        .that()
        .resideInAPackage("$INFRASTRUCTURE.internal..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("$INFRASTRUCTURE.external..")
        .check(classes)

    @ArchTest
    fun `Datasource sub folder can access entity`(classes: JavaClasses) =
        noClasses()
            .that()
            .resideInAPackage("$INFRASTRUCTURE.external.datasource..entity..")
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideOutsideOfPackages("$INFRASTRUCTURE.external.datasource..")
            .check(classes)

    @ArchTest
    fun `Controllers should implement contract interface`(classes: JavaClasses) =
        noClasses()
            .that()
            .areAnnotatedWith(RestController::class.java)
            .should()
            .notImplement(resideInAnyPackage("$INFRASTRUCTURE.internal.web.."))
            .check(classes)

    @ArchTest
    fun `Repositories should reside in Datasource`(classes: JavaClasses) =
        noClasses()
            .that()
            .areAnnotatedWith(Repository::class.java)
            .should()
            .resideOutsideOfPackages("$INFRASTRUCTURE.external.datasource..")
            .check(classes)

    @ArchTest
    fun `Services should reside in Domain's or Datasource's Services`(classes: JavaClasses) =
        noClasses()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .resideOutsideOfPackages(
                "$DOMAIN.service..",
                "$INFRASTRUCTURE.external.datasource.."
            )
            .check(classes)

    @ArchTest
    fun `Services should implement contract interface`(classes: JavaClasses) =
        noClasses()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .resideOutsideOfPackages(
                "$DOMAIN.service..",
                "$INFRASTRUCTURE.external.datasource..repository.impl..",
            )
            .check(classes)

    @ArchTest
    fun `Backend package should only contain specified sub-packages`(classes: JavaClasses) = noClasses()
        .that()
        .resideInAPackage("$BACKEND..")
        .and()
        .resideOutsideOfPackages(
            "$CONFIG..",
            "$DOMAIN..",
            "$INFRASTRUCTURE.."
        )
        .should()
        .resideInAnyPackage()
        .check(classes)

    @ArchTest
    fun `Configuration should end with Config`(classes: JavaClasses) = classes()
        .that()
        .resideInAnyPackage("$CONFIG..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Config")
        .check(classes)

    @ArchTest
    fun `Annotation should by tagged with Target and Retention`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.annotation..")
        .should()
        .beAnnotatedWith(Target::class.java)
        .andShould()
        .beAnnotatedWith(Retention::class.java)
        .check(classes)

    @ArchTest
    fun `Handler should end with Handler`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.handler..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Handler")
        .check(classes)

    @ArchTest
    fun `Constant should end with Const`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.constant..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Const")
        .check(classes)

    @ArchTest
    fun `Enumeration should end with Enum`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.enumeration..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Enum")
        .check(classes)

    @ArchTest
    fun `Extension should end with Ext`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.extension..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Ext")
        .check(classes)

    @ArchTest
    fun `Model should end with Model`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.model..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Model")
        .check(classes)

    @ArchTest
    fun `Model Repository should end with ModelRepository`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.repository..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("ModelRepository")
        .check(classes)

    @ArchTest
    fun `Service should end with Service`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.service..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Service")
        .check(classes)

    @ArchTest
    fun `Validator should end with Validator`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$DOMAIN.validator..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Validator")
        .check(classes)

    @ArchTest
    fun `Entity Repository should end with EntityRepository`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$INFRASTRUCTURE.external.datasource.postgres.repository..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Repository")
        .orShould()
        .haveSimpleNameEndingWith("Queries")
        .orShould()
        .haveSimpleNameEndingWith("Fields")
        .check(classes)

    @ArchTest
    fun `Controller should end with Controller`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$INFRASTRUCTURE.internal.web.controller..")
        .and().areTopLevelClasses()
        .should()
        .haveNameMatching(".*Controller(Advice)?")
        .check(classes)

    @ArchTest
    fun `Dto should end with Dto`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$INFRASTRUCTURE.internal.web.dto..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Dto")
        .check(classes)

    @ArchTest
    fun `DtoMapper should end with DtoMapper`(classes: JavaClasses) = classes()
        .that()
        .resideInAPackage("$INFRASTRUCTURE.internal.web.mapper..")
        .and().areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("DtoMapper")
        .check(classes)
}
