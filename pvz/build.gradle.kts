plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "io.allitov"
version = "0.0.1-SNAPSHOT"
description = "pvz"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.sqlobject)
    implementation(libs.jdbi3.postgres)
    implementation(libs.jdbi3.spring5)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    compileOnly(libs.lombok)
    runtimeOnly(libs.postgresql)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.starter.thymeleaf.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testCompileOnly(libs.lombok)
    testRuntimeOnly(libs.junit.launcher)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}