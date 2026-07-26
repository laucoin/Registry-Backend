package fr.laucoin.registry.backend.domain.handler

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import fr.laucoin.registry.backend.domain.annotation.HttpCacheable
import org.springframework.security.access.prepost.PreAuthorize

/**
 * The CacheHeadersHandler 304 short-circuit answers before the controller
 * method is invoked, so @PreAuthorize on a cacheable endpoint would be
 * silently skipped — the combination must never appear.
 */
@AnalyzeClasses(
	packages = ["fr.laucoin.registry.backend"],
	importOptions = [DoNotIncludeTests::class],
)
class HttpCacheableArchitectureTest {
	@ArchTest
	fun `HttpCacheable endpoints should not carry PreAuthorize`(classes: JavaClasses) = noMethods()
		.that()
		.areAnnotatedWith(HttpCacheable::class.java)
		.should()
		.beAnnotatedWith(PreAuthorize::class.java)
		.orShould()
		.beDeclaredInClassesThat()
		.areAnnotatedWith(PreAuthorize::class.java)
		.check(classes)
}
