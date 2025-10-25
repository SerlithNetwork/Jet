plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.4"
}

group = "net.serlith"
version = "0.0.1-SNAPSHOT"
description = "Jet"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.google.protobuf:protobuf-javalite:3.17.3")
    implementation("com.google.protobuf:protobuf-java-util:3.14.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")
    implementation("org.xhtmlrenderer:flying-saucer-core:10.0.3") // Close your eyes before opening: https://flyingsaucerproject.github.io/flyingsaucer/r8/guide/users-guide-R8.html
    implementation("io.github.neatguycoding:netty-socketio-core:3.0.0")
    implementation("io.github.neatguycoding:netty-socketio-spring:3.0.0")
    implementation("io.github.neatguycoding:netty-socketio-spring-boot-starter:3.0.0")

    runtimeOnly("com.zaxxer:HikariCP:6.2.1")
    runtimeOnly("com.mysql:mysql-connector-j:9.2.0")
    runtimeOnly("com.h2database:h2:2.4.240")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.security:spring-security-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.8.0"
    }
    generateProtoTasks {
        all().configureEach {
            builtins.first { it.name == "java" }.option("lite")
        }
    }
    generatedFilesBaseDir = "$projectDir/src/generated"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
