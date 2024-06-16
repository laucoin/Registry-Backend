plugins {
    id("jacoco")
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("net.researchgate.release") version "3.0.2"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
}

group = "com.laucoin"

// Features tools versions
val apacheCommonVersion = "1.12.0"
val gsonVersion = "2.11.0"

// Swagger
val swaggerVersion = "2.5.0"

// Tests versions
val jacocoVersion = "0.8.12"
val mockitoVersion = "5.12.0"
val mockitoKotlinVersion = "5.3.1"
val testContainerVersion = "1.19.8"
val tuTarget = BigDecimal(0.9)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Feature related
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.apache.commons:commons-text:$apacheCommonVersion")

    // Data
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$swaggerVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
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
    finalizedBy(tasks.jacocoTestCoverageVerification, tasks.jacocoTestReport)
}

release {
    versionPropertyFile.set("version.properties")
    preTagCommitMessage.set("[skip ci][Gradle Release Plugin] - pre tag commit: ")
    tagCommitMessage.set("[skip ci][Gradle Release Plugin] - creating tag: ")
    newVersionCommitMessage.set("[Gradle Release Plugin] - new version: ")
    buildTasks.set(emptyList<String>())
    git {
        requireBranch.set("develop")
    }
}
