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
	fun `Infrastructure out should not depend on infrastructure in`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.out..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("$INFRASTRUCTURE.in..")
		.check(classes)

	@ArchTest
	fun `Postgres sub folder can access entity`(classes: JavaClasses) =
		noClasses()
			.that()
			.resideInAPackage("$INFRASTRUCTURE.in.postgres..entity..")
			.should()
			.onlyBeAccessed()
			.byClassesThat()
			.resideOutsideOfPackages("$INFRASTRUCTURE.in.postgres..")
			.check(classes)

	@ArchTest
	fun `Controllers should implement contract interface`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(RestController::class.java)
			.should()
			.notImplement(resideInAnyPackage("$INFRASTRUCTURE.out.api.."))
			.check(classes)

	@ArchTest
	fun `Repositories should reside in Postgres`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(Repository::class.java)
			.should()
			.resideOutsideOfPackages("$INFRASTRUCTURE.in.postgres..")
			.check(classes)

	@ArchTest
	fun `Services should reside in Domain's, Postgres's Services or Adapter`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(Service::class.java)
			.should()
			.resideOutsideOfPackages(
				"$DOMAIN.service..",
				"$INFRASTRUCTURE.in.postgres..",
				"$INFRASTRUCTURE.in..adapter.."
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
				"$INFRASTRUCTURE.in.postgres..repository.impl..",
				"$INFRASTRUCTURE.in..adapter..",
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
		.orShould()
		.haveSimpleNameEndingWith("Exception")
		.check(classes)

	@ArchTest
	fun `Port should end with Port`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$DOMAIN.port..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Port")
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
		.resideInAPackage("$INFRASTRUCTURE.in.postgres..repository..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Repository")
		.orShould()
		.haveSimpleNameEndingWith("Queries")
		.orShould()
		.haveSimpleNameEndingWith("Fields")
		.check(classes)

	@ArchTest
	fun `Adapter should end with Adapter`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.in..adapter..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Adapter")
		.check(classes)

	@ArchTest
	fun `Controller should end with Controller`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.out.api.controller..")
		.and().areTopLevelClasses()
		.should()
		.haveNameMatching(".*Controller(Advice)?")
		.check(classes)

	@ArchTest
	fun `Dto should end with Dto`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.out.api.dto..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Dto")
		.check(classes)

	@ArchTest
	fun `DtoMapper should end with DtoMapper`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.out.api.mapper..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("DtoMapper")
		.check(classes)
}
