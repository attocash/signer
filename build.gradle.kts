import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.1.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion

    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

group = "cash.atto"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ktlint {
    // https://github.com/JLLeitschuh/ktlint-gradle/issues/809
    version.set("1.4.1")
}

ext["kotlin-coroutines.version"] = "1.9.0" // TEMP. Until spring updates
ext["kotlin-serialization.version"] = "1.8.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    val commonsVersion = "5.1.1"
    val cucumberVersion = "7.23.0"

    implementation("cash.atto:commons-core:$commonsVersion")
    implementation("com.google.cloud:google-cloud-kms:2.75.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.10")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.github.oshai:kotlin-logging:7.0.7")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("org.junit.platform:junit-platform-suite") // for cucumber
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    environment("GRADLE", "true")
    useJUnitPlatform()
}

ktlint {
    version.set("1.4.1")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("--static")
            buildArgs.add("--libc=musl")
            buildArgs.add("--no-server")
            buildArgs.add("--strict-image-heap")
        }
    }
}
