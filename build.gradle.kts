plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.6"
    id("org.jooq.jooq-codegen-gradle") version "3.19.32"
    id("org.flywaydb.flyway") version "12.8.1"
}

group = "net.serlith"
version = "0.0.1-SNAPSHOT"
description = "Jet"

val nettyVersion = "4.2.10.Final"
val jooqVersion = "3.19.32"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    jooqCodegen("org.jooq:jooq-codegen:$jooqVersion")
    jooqCodegen("org.jooq:jooq-meta-extensions:$jooqVersion")
    jooqCodegen("org.postgresql:postgresql")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("software.amazon.awssdk:s3:2.42.41")
    implementation("software.amazon.awssdk:netty-nio-client:2.42.41")
    implementation("com.google.protobuf:protobuf-java:4.34.0")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.r2dbc:r2dbc-pool")

    // Reactive databases
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.84")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Netty transport natives
    runtimeOnly("io.netty:netty-transport-native-io_uring:$nettyVersion:linux-x86_64")
    runtimeOnly("io.netty:netty-transport-native-io_uring:$nettyVersion:linux-aarch_64")
    runtimeOnly("io.netty:netty-transport-native-kqueue:$nettyVersion:osx-x86_64")
    runtimeOnly("io.netty:netty-transport-native-kqueue:$nettyVersion:osx-aarch_64")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:12.8.1")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.34.0"
    }
}

val script = projectDir.resolve("src/main/resources/db/migration/V1__init_schema.sql")

flyway {
    url = property("jet.jooq.database.url") as String
    user = property("jet.jooq.database.user") as String
    password = property("jet.jooq.database.password") as String
    locations = arrayOf("filesystem:${script.parent}")
    driver = "org.postgresql.Driver"

    dependencies {
        runtimeOnly("org.postgresql:postgresql")
    }
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = property("jet.jooq.database.url") as String
            user = property("jet.jooq.database.user") as String
            password = property("jet.jooq.database.password") as String
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
            }
            generate {
                isRecords = true
            }
            target {
                packageName = "net.serlith.jet.schema"
                directory = "build/generated-src/jooq/main"
            }
        }
    }
}

sourceSets {
    main {
        java.srcDir("build/generated-src/jooq/main")
    }
}

tasks {
    build {
        dependsOn(generateProto)
        dependsOn(jooqCodegen)
    }
    jooqCodegen {
        dependsOn(flywayClean)
    }
}
