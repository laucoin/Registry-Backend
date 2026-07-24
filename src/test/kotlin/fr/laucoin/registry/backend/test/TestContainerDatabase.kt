package fr.laucoin.registry.backend.test

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class TestContainerDatabase: ApplicationContextInitializer<ConfigurableApplicationContext> {
	private companion object {
		private const val CONFIG_PREFIX = "registry.datasource."

		private const val DB_USERNAME = "backend"
		private const val DB_PASSWORD = "test123"
		private const val DB_REGISTRY = "registry"
		private const val DB_SCHEMAS = "public"
		private const val SERVICE_PORT = 5432

		private val container = GenericContainer("postgres:18-alpine")
			.withExposedPorts(SERVICE_PORT)
			.withEnv("PGUSER", DB_USERNAME)
			.withEnv("POSTGRES_USER", DB_USERNAME)
			.withEnv("POSTGRES_PASSWORD", DB_PASSWORD)
			.withEnv("POSTGRES_DB", DB_REGISTRY)
			.withCommand("postgres", "-c", "max_connections=500")
			.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
	}

	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		container.start()
		TestPropertyValues.of(
			"${CONFIG_PREFIX}base-url=${dbUrl()}",
			"${CONFIG_PREFIX}database=$DB_REGISTRY",
			"${CONFIG_PREFIX}username=$DB_USERNAME",
			"${CONFIG_PREFIX}password=$DB_PASSWORD",
			"${CONFIG_PREFIX}schemas=$DB_SCHEMAS",
		).applyTo(applicationContext)
	}

	private fun dbUrl(): String = "${container.host}:${container.getMappedPort(SERVICE_PORT)}"
}
