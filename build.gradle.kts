import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
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
import java.time.Duration
import java.util.Properties

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("io.zonky.test:embedded-postgres:2.2.2")
		classpath("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64:18.6.0")
		classpath("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64-alpine:18.6.0")
		classpath("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8:18.6.0")
		classpath("org.flywaydb:flyway-core:12.4.0")
		classpath("org.flywaydb:flyway-database-postgresql:12.4.0")
		classpath("org.postgresql:postgresql:42.7.13")
	}
}

plugins {
	kotlin("jvm") version "2.4.20"
	kotlin("plugin.spring") version "2.4.20"
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
	implementation("org.jooq:jooq:$jooqVersion")
	implementation("org.jooq:jooq-kotlin:$jooqVersion")
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
				}
				generator {
					name = "org.jooq.codegen.KotlinGenerator"
					database {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						excludes = "flyway_schema_history"
						forcedTypes {
							forcedType {
								userType = "java.time.ZonedDateTime"
								converter =
									"fr.laucoin.registry.backend.infrastructure.driven.postgres.converter.ZonedDateTimeConverter"
								includeTypes = "TIMESTAMPTZ"
							}
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
						isImplicitJoinPathsToOne = false
						isImplicitJoinPathsToMany = false
						isImplicitJoinPathsManyToMany = false
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
	var pg: EmbeddedPostgres? = null

	doFirst {
		val instance = EmbeddedPostgres.builder()
			.setPGStartupWait(Duration.ofSeconds(30))
			.start()
		pg = instance

		val jdbcUrl = instance.getJdbcUrl("postgres", "postgres")

		Flyway.configure()
			.dataSource(jdbcUrl, "postgres", "postgres")
			.locations("filesystem:${project.projectDir}/src/main/resources/db/migrations")
			.load()
			.migrate()

		val jdbc = project.extensions.getByType<JooqExtension>().configurations.getByName("main").jooqConfiguration.jdbc
		jdbc.url = jdbcUrl
		jdbc.user = "postgres"
		jdbc.password = "postgres"
	}

	doLast {
		pg?.close()
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
	jvmArgs("--enable-native-access=ALL-UNNAMED")
	testLogging {
		events = setOf(FAILED, SKIPPED)
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStandardStreams = false
	}
	finalizedBy(tasks.named("koverVerify"), tasks.named("koverHtmlReport"))
}

tasks.withType<BootJar>().configureEach {
	val targetName = project.findProperty("targetName") as String?
	archiveFileName.set(targetName ?: "registry-backend.jar")
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
}