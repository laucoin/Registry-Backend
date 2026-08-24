import java.util.Properties
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm") version "2.4.10"
	kotlin("plugin.spring") version "2.4.10"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jetbrains.kotlinx.kover") version "0.9.9"
}

group = "fr.laucoin.registry"

val versionProperties = Properties().apply {
	rootProject.file("version.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
version = versionProperties.getProperty("version", "0.0.1-SNAPSHOT")

// External libraries 📚
val apacheTextVersion = "1.15.0"
val swaggerVersion = "3.1.0"

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

	// Documentation 📚
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$swaggerVersion")

	// Data 💾
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.apache.commons:commons-text:$apacheTextVersion")
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

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