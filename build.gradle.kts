import nu.studer.gradle.jooq.JooqEdition
import nu.studer.gradle.jooq.JooqExtension
import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.core.Flyway
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.kotlin.database
import org.jooq.meta.kotlin.forcedType
import org.jooq.meta.kotlin.forcedTypes
import org.jooq.meta.kotlin.generate
import org.jooq.meta.kotlin.generator
import org.jooq.meta.kotlin.jdbc
import org.jooq.meta.kotlin.target
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.Properties

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		// Used only inside the generateJooq task's doFirst block, to spin up an ephemeral
		// Postgres, migrate it, then point jOOQ codegen at it — see the `jooq {}` block below.
		classpath("org.testcontainers:testcontainers:2.0.5")
		classpath("org.flywaydb:flyway-core:12.4.0")
		classpath("org.flywaydb:flyway-database-postgresql:12.4.0")
		classpath("org.postgresql:postgresql:42.7.13")
	}
}

plugins {
	kotlin("jvm") version "2.4.10"
	kotlin("plugin.spring") version "2.4.10"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jetbrains.kotlinx.kover") version "0.9.9"
	id("nu.studer.jooq") version "10.2.1"
}

group = "fr.laucoin.registry"

val versionProperties = Properties().apply {
	rootProject.file("version.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
version = versionProperties.getProperty("version", "0.0.1-SNAPSHOT")

// External libraries 📚
val apacheTextVersion = "1.15.0"
val swaggerVersion = "3.1.1"
val caffeineVersion = "3.2.4"
val jooqVersion = "3.21.7" // must match the version Spring Boot's BOM forces org.jooq:jooq to

// Testing 🧪
val mockWebServer = "5.5.0"
val testArch = "1.5.0"
val mockitoKotlinVersion = "6.3.0"
val testContainerVersion = "2.0.5"

kotlin {
	jvmToolchain(25)
	compilerOptions {
		freeCompilerArgs.addAll(
			"-Xjsr305=strict",
			"-opt-in=kotlin.RequiresOptIn"
		)
	}
	sourceSets.main {
		kotlin.srcDir("build/generated-src/jooq/main")
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Security 🔒
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

	// Web 👨‍💻
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// Kotlin ♥️
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Monitoring & Observability 👀
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Reactive context propagation (e.g. LocaleContext across WebFlux thread hops) 🔀
	implementation("io.micrometer:context-propagation")

	// Documentation 📚
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$swaggerVersion")

	// Data 💾
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
	implementation("org.apache.commons:commons-text:$apacheTextVersion")
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// jOOQ 🧬 — type-safe SQL DSL, generated from the schema (see the `jooq {}` block below)
	implementation("org.jooq:jooq:$jooqVersion")
	implementation("org.jooq:jooq-kotlin:$jooqVersion")

	// jOOQ codegen worker classpath only — needs the JDBC driver to introspect the ephemeral
	// codegen Postgres container (see generateJooq's doFirst below)
	jooqGenerator("org.postgresql:postgresql")

	// Test 🧪
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-webtestclient")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServer")
	testImplementation("com.tngtech.archunit:archunit-junit5:$testArch")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
	testImplementation("org.testcontainers:testcontainers:$testContainerVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jooq {
	version.set(jooqVersion)
	edition.set(JooqEdition.OSS)

	configurations {
		create("main") {
			generateSchemaSourceOnCompilation.set(true)

			jooqConfiguration {
				logging = Logging.WARN
				jdbc {
					driver = "org.postgresql.Driver"
					// url/user/password are set at task-execution time in generateJooq's
					// doFirst below, once the ephemeral codegen container is up.
				}
				generator {
					name = "org.jooq.codegen.KotlinGenerator"
					database {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						excludes = "flyway_schema_history"
						forcedTypes {
							// Every TIMESTAMP WITH TIME ZONE column -> ZonedDateTime (every entity's type),
							// instead of jOOQ's default OffsetDateTime — avoids a per-query conversion.
							forcedType {
								userType = "java.time.ZonedDateTime"
								converter =
									"fr.laucoin.registry.backend.infrastructure.driven.postgres.converter.ZonedDateTimeConverter"
								includeTypes = "TIMESTAMPTZ"
							}
							// One entry per Postgres VARCHAR column that's actually enum-shaped — binds it
							// straight to the existing domain Kotlin enum via jOOQ's built-in EnumConverter
							// (name-based, same as the R2DBC driver's default conversion today). Deliberately
							// per-column (includeExpression), never a blanket includeTypes = "VARCHAR": most
							// VARCHAR columns (names, emails, roles) are plain text.
							mapOf(
								"tb_user\\.type" to "UserTypeEnum",
								"tb_participant\\.type" to "ParticipantTypeEnum",
								"tb_movement\\.type" to "MovementTypeEnum",
								"tb_movement\\.reason" to "MovementReasonEnum",
								"tb_alert\\.status" to "AlertStatusEnum",
								"tb_project_profile\\.status" to "ProfileStatusEnum",
								"tb_preferences\\.theme" to "ThemeEnum",
							).forEach { (tableDotColumn, enumSimpleName) ->
								forcedType {
									userType = "fr.laucoin.registry.backend.domain.enumeration.$enumSimpleName"
									isEnumConverter = true
									includeExpression = tableDotColumn
									includeTypes = "VARCHAR"
								}
							}
							forcedType {
								userType =
									"kotlin.collections.List<fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum>"
								converter =
									"fr.laucoin.registry.backend.infrastructure.driven.postgres.converter.ProjectOptionArrayConverter"
								includeExpression = "tb_project\\.options"
							}
						}
					}
					generate {
						isRecords = true
						isPojos = false
						isFluentSetters = true
						isDeprecated = false
					}
					target {
						packageName = "fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq"
						directory = "build/generated-src/jooq/main"
					}
				}
			}
		}
	}
}

tasks.named<JooqGenerate>("generateJooq") {
	var codegenContainer: GenericContainer<*>? = null

	doFirst {
		val container = GenericContainer("postgres:18-alpine")
			.withExposedPorts(5432)
			.withEnv("POSTGRES_USER", "jooq")
			.withEnv("POSTGRES_PASSWORD", "jooq")
			.withEnv("POSTGRES_DB", "jooq")
			.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
		container.start()
		codegenContainer = container

		val jdbcUrl = "jdbc:postgresql://${container.host}:${container.getMappedPort(5432)}/jooq"

		Flyway.configure()
			.dataSource(jdbcUrl, "jooq", "jooq")
			.locations("filesystem:${project.projectDir}/src/main/resources/db/migrations")
			.load()
			.migrate()

		val jdbc = project.extensions.getByType<JooqExtension>().configurations.getByName("main").jooqConfiguration.jdbc
		jdbc.url = jdbcUrl
		jdbc.user = "jooq"
		jdbc.password = "jooq"
	}

	doLast {
		codegenContainer?.stop()
	}
}

tasks.named("compileKotlin") {
	dependsOn("generateJooq")
}

kover {
	reports {
		filters {
			excludes {
				packages("fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq")
			}
		}
	}
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	maxParallelForks = Runtime.getRuntime().availableProcessors()
	testLogging {
		events = setOf(FAILED, SKIPPED)
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStandardStreams = false
	}
	finalizedBy(tasks.named("koverVerify"), tasks.named("koverHtmlReport"))
}

tasks.withType<BootJar>().configureEach {
	val targetName: String? by project
	archiveFileName.set(targetName ?: "registry-backend.jar")
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
}