package fr.laucoin.registry.backend.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards that the huge dev seed (src/main/resources/db/seed/R__huge_test_dataset.sql)
 * is loaded ONLY by the `local` and `dev` profiles, and NEVER by the default (prod)
 * profile — enforced purely through `spring.flyway.locations`. The seed file ships in
 * the artifact (needed by a deployed `dev`), so this invariant is the only thing keeping
 * it out of production; this test fails loudly if a future change leaks it in.
 *
 * `e2e` is held to both halves: it disables the rate limiter (the only profile allowed
 * to) and must NOT load the seed, because Registry-E2E seeds its own fixtures and the
 * dataset would TRUNCATE tb_user underneath them.
 */
class FlywaySeedLocationTest {
	/**
	 * Reads the raw entries rather than `stringPropertyNames()`: SnakeYAML types
	 * `capacity: 0` as an Integer, which that view silently drops.
	 */
	private fun properties(yaml: String): Map<String, String> {
		val factory = YamlPropertiesFactoryBean()
		factory.setResources(ClassPathResource(yaml))
		val properties = factory.getObject() ?: error("Unable to load $yaml")
		return properties.entries.associate { it.key.toString() to it.value.toString() }
	}

	private fun flywayLocations(yaml: String): List<String> =
		properties(yaml).filterKeys { it.startsWith("spring.flyway.locations") }.values.toList()

	private fun rateLimitCapacities(yaml: String): List<String> =
		properties(yaml)
			.filterKeys { it.startsWith("registry.security.rate-limit") && it.endsWith("capacity") }
			.values.toList()

	@Test
	fun `Should not load the seed dataset in the default (prod) profile`() {
		// Arrange
		val yaml = "application.yml"

		// Act
		val locations = flywayLocations(yaml)

		// Assert
		assertTrue(locations.any { it.contains("db/migrations") }, "The default profile must run versioned migrations")
		assertFalse(
			locations.any { it.contains("db/seed") },
			"The huge seed dataset must never be loaded by the default/prod profile",
		)
	}

	@Test
	fun `Should load the seed dataset in the local and dev profiles`() {
		// Arrange
		val yamls = listOf("application-local.yml", "application-dev.yml")

		// Act
		val locations = yamls.associateWith { flywayLocations(it) }

		// Assert
		yamls.forEach {
			assertTrue(locations.getValue(it).any { location -> location.contains("db/seed") }, it)
		}
	}

	/**
	 * Registry-E2E stands the stack up on the versioned migrations and seeds its own
	 * fixtures; the dataset's TRUNCATE would wipe them, so `e2e` must stay seed-free.
	 */
	@Test
	fun `Should not load the seed dataset in the e2e profile`() {
		// Arrange
		val yaml = "application-e2e.yml"

		// Act
		val locations = flywayLocations(yaml)

		// Assert
		assertTrue(locations.none { it.contains("db/seed") }, "The e2e profile must not load the seed dataset")
	}

	@Test
	fun `Should disable the rate limiter in the e2e profile only`() {
		// Arrange
		val yamls = listOf("application.yml", "application-local.yml", "application-dev.yml")

		// Act
		val e2eCapacities = rateLimitCapacities("application-e2e.yml")

		// Assert
		assertTrue(e2eCapacities.isNotEmpty(), "The e2e profile must disable the rate limiter")
		e2eCapacities.forEach { assertEquals("0", it) }
		yamls.forEach {
			assertTrue(rateLimitCapacities(it).isEmpty(), "\"$it\" must not weaken the rate limiter")
		}
	}
}
