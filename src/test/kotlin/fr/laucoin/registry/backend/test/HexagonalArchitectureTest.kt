package fr.laucoin.registry.backend.test

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
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
	private companion object {
		private const val BACKEND = "fr.laucoin.registry.backend"
		private const val CONFIG = "$BACKEND.config"
		private const val DOMAIN = "$BACKEND.domain"
		private const val INFRASTRUCTURE = "$BACKEND.infrastructure"
		private const val SERVICE = "$DOMAIN.service"
		private const val CORE = "$SERVICE.core"
		private const val USECASE = "$SERVICE.usecase"
		private const val SUPPORT = "$SERVICE.support"
		private const val DOMAIN_HELPER = "$DOMAIN.helper"
		private const val INFRASTRUCTURE_HELPER = "$INFRASTRUCTURE.helper"
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
	fun `Infrastructure driving should not depend on infrastructure driven`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driving..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("$INFRASTRUCTURE.driven..")
		.check(classes)

	@ArchTest
	fun `Postgres sub folder can access entity`(classes: JavaClasses) =
		noClasses()
			.that()
			.resideInAPackage("$INFRASTRUCTURE.driven.postgres..entity..")
			.should()
			.onlyBeAccessed()
			.byClassesThat()
			.resideOutsideOfPackages("$INFRASTRUCTURE.driven.postgres..")
			.check(classes)

	@ArchTest
	fun `Controllers should implement contract interface`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(RestController::class.java)
			.should()
			.notImplement(resideInAnyPackage("$INFRASTRUCTURE.driving.api.."))
			.check(classes)

	@ArchTest
	fun `Repositories should reside in Postgres`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(Repository::class.java)
			.should()
			.resideOutsideOfPackages("$INFRASTRUCTURE.driven.postgres..")
			.check(classes)

	@ArchTest
	fun `Services should reside in Domain's, Postgres's Services or Adapter`(classes: JavaClasses) =
		noClasses()
			.that()
			.areAnnotatedWith(Service::class.java)
			.should()
			.resideOutsideOfPackages(
				"$DOMAIN.service..",
				"$INFRASTRUCTURE.driven.postgres..",
				"$INFRASTRUCTURE.driven..adapter.."
			)
			.check(classes)

	@ArchTest
	fun `Domain services should implement a domain contract interface`(classes: JavaClasses) =
		classes()
			.that()
			.areAnnotatedWith(Service::class.java)
			.and()
			.resideInAPackage("$DOMAIN.service..")
			.should()
			.implement(resideInAnyPackage(SERVICE, CORE, USECASE, SUPPORT))
			.check(classes)

	@ArchTest
	fun `Domain should not depend on infrastructure or config`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$DOMAIN..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("$INFRASTRUCTURE..", "$CONFIG..")
		.check(classes)

	@ArchTest
	fun `Domain should not depend on web adapters`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$DOMAIN..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(
			"org.springframework.web..",
			"org.springframework.http.server..",
			"org.springframework.security.web..",
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
		.resideInAPackage("$INFRASTRUCTURE.driving.api.validation.annotation..")
		.should()
		.beAnnotatedWith(Target::class.java)
		.andShould()
		.beAnnotatedWith(Retention::class.java)
		.check(classes)

	@ArchTest
	fun `Handler should end with Handler`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driving.security.handler..")
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
		.resideInAPackage("$SERVICE..")
		.and().resideOutsideOfPackage("$USECASE..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Service")
		.check(classes)

	@ArchTest
	fun `Validator should end with Validator`(classes: JavaClasses) = classes()
		.that()
		.resideInAnyPackage(
			"$INFRASTRUCTURE.driving.security.validator..",
			"$INFRASTRUCTURE.driving.api.validation.validator..",
		)
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Validator")
		.check(classes)

	@ArchTest
	fun `Entity Repository should end with EntityRepository`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driven.postgres..repository..")
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
		.resideInAPackage("$INFRASTRUCTURE.driven..adapter..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Adapter")
		.check(classes)

	@ArchTest
	fun `Controller should end with Controller`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driving.api.controller..")
		.and().areTopLevelClasses()
		.should()
		.haveNameMatching(".*Controller(Advice)?")
		.check(classes)

	@ArchTest
	fun `Dto should end with Dto`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driving.api.dto..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Dto")
		.check(classes)

	@ArchTest
	fun `DtoMapper should end with DtoMapper`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$INFRASTRUCTURE.driving.api.mapper..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("DtoMapper")
		.check(classes)

	@ArchTest
	fun `UseCase should end with UseCase`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$USECASE..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("UseCase")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Core services should not depend on another core service`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$CORE..")
		.should(notDependOnAnotherServiceOf(CORE))
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `UseCases should not depend on another UseCase`(classes: JavaClasses) = classes()
		.that()
		.resideInAPackage("$USECASE..")
		.should(notDependOnAnotherServiceOf(USECASE))
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Core and support services should not depend on UseCases`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAnyPackage("$CORE..", "$SUPPORT..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("$USECASE..")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Support services should not depend on core services`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$SUPPORT..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("$CORE..")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `UseCases should only be used by driving adapters`(classes: JavaClasses) = noClasses()
		.that()
		.resideOutsideOfPackages("$INFRASTRUCTURE.driving..", "$USECASE..", "$CONFIG..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("$USECASE..")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Infrastructure driving should not depend on core services`(classes: JavaClasses) = freeze(
		noClasses()
			.that()
			.resideInAPackage("$INFRASTRUCTURE.driving..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage("$CORE..")
			.allowEmptyShould(true)
	).check(classes)

	@ArchTest
	fun `Domain helper should not depend on services, infrastructure or config`(classes: JavaClasses) = noClasses()
		.that()
		.resideInAPackage("$DOMAIN_HELPER..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("$SERVICE..", "$INFRASTRUCTURE..", "$CONFIG..")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Helper should end with Helper`(classes: JavaClasses) = classes()
		.that()
		.resideInAnyPackage("$DOMAIN_HELPER..", "$INFRASTRUCTURE_HELPER..")
		.and().areTopLevelClasses()
		.should()
		.haveSimpleNameEndingWith("Helper")
		.allowEmptyShould(true)
		.check(classes)

	@ArchTest
	fun `Helper should reside in a helper package`(classes: JavaClasses) = classes()
		.that()
		.haveSimpleNameEndingWith("Helper")
		.and().areTopLevelClasses()
		.should()
		.resideInAnyPackage("$DOMAIN_HELPER..", "$INFRASTRUCTURE_HELPER..")
		.allowEmptyShould(true)
		.check(classes)

	private fun notDependOnAnotherServiceOf(servicePackage: String) =
		object : ArchCondition<JavaClass>("not depend on another service of $servicePackage..") {
			override fun check(item: JavaClass, events: ConditionEvents) {
				val owner = item.outermostClass()
				item.directDependenciesFromSelf
					.filter { it.targetClass.packageName.startsWith(servicePackage) }
					.filter { it.targetClass.outermostClass() != owner }
					.filter { it.targetClass.outermostClass() !in owner.allRawInterfaces }
					.forEach { events.add(SimpleConditionEvent.violated(item, it.description)) }
			}
		}

	private fun JavaClass.outermostClass(): JavaClass = enclosingClass.map { it.outermostClass() }.orElse(this)
}
