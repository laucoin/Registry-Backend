plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("net.researchgate.release") version "3.1.0"
    id("jacoco")
}

group = "fr.laucoin.registry"

val swaggerVersion = "2.7.0"
val apacheCommonVersion = "1.13.0"
val jacocoVersion = "0.8.12"
val mockitoVersion = "5.14.2"
val testArch = "1.3.0"
val mockWebServer = "5.0.0-alpha.14"
val mockitoKotlinVersion = "5.4.0"
val testContainerVersion = "1.20.4"

val tuTarget = BigDecimal(0.8)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$swaggerVersion")

    implementation("org.apache.commons:commons-text:$apacheCommonVersion")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServer")
    testImplementation("com.tngtech.archunit:archunit-junit5:$testArch")
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
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    finalizedBy(tasks.jacocoTestCoverageVerification, tasks.jacocoTestReport)
}

release {
    versionPropertyFile.set("version.properties")
    preTagCommitMessage.set("[skip ci][Gradle Release Plugin] - pre tag commit: ")
    tagCommitMessage.set("[skip ci][Gradle Release Plugin] - creating tag: ")
    newVersionCommitMessage.set("[skip ci][Gradle Release Plugin] - new version: ")
    buildTasks.set(emptyList<String>())
    git {
        requireBranch.set("develop")
    }
}
