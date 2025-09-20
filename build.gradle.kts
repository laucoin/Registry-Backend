import java.util.Properties
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm") version "2.1.0"
	kotlin("plugin.spring") version "2.1.0"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("jacoco")
}

group = "fr.laucoin.registry"
val versionFile = rootProject.file("version.properties")
val versionProperties: Properties = Properties()
versionProperties.load(versionFile.inputStream())
version = versionProperties.getProperty("version")

val apacheTextVersion = "1.14.0"
val prometheusVersion = "1.15.2"
val swaggerVersion = "2.8.9"
val mockWebServer = "5.1.0"
val testArch = "1.4.1"
val mockitoKotlinVersion = "6.0.0"
val testContainerVersion = "1.21.3"
val jacocoVersion = "0.8.13"

val tuTarget = BigDecimal(0.74)

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
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
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Data 💾
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.apache.commons:commons-text:$apacheTextVersion")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// Monitoring & Observability 👀
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

	// Documentation 📚
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$swaggerVersion")

	// Test 🧪
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServer")
	testImplementation("com.tngtech.archunit:archunit-junit5:$testArch")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
	testImplementation("org.testcontainers:testcontainers:$testContainerVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

jacoco {
	toolVersion = jacocoVersion
}

tasks.jacocoTestCoverageVerification {
	violationRules {
		rule {
			limit {
				minimum = tuTarget
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	maxParallelForks = Runtime.getRuntime().availableProcessors()
	finalizedBy(tasks.jacocoTestCoverageVerification, tasks.jacocoTestReport)
}

val targetName: String? by project

tasks.withType<BootJar> {
	archiveFileName.set(targetName ?: "registry-backend.jar")
}
