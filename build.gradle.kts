import java.time.Duration

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "com.moloco.mcm"
version = "0.1.0"
description = "Java library for sending user events to Moloco MCM's User Event API endpoint"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    // toolchain {
    //     languageVersion.set(JavaLanguageVersion.of(11))
    // }
}

dependencies {
    // Core dependencies
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

// Test Configuration
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo Configuration
tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(false)
        csv.required.set(false)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.8".toBigDecimal()
            }
        }

        rule {
            element = "CLASS"
            excludes = listOf("*.Test*")

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.8".toBigDecimal()
            }

            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

// Javadoc Configuration
tasks.javadoc {
    exclude("com/moloco/mcm/Main.java")
    options {
        encoding = "UTF-8"
        (this as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addBooleanOption("html5", true)

            // Add project information
            windowTitle = "$project.name $project.version API"
            docTitle = "$project.name $project.version API Documentation"
            bottom = "Copyright © 2024 Moloco, Inc. Built with JDK 11"

            // Add links to Java SE and other dependencies' Javadoc
            links = listOf(
                "https://docs.oracle.com/en/java/javase/11/docs/api/",
                "https://javadoc.io/doc/org.apache.httpcomponents.client5/httpclient5/5.4/index.html",
                "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/2.18.1/index.html",
                "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/2.18.1/index.html",
                "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-core/2.18.1/index.html"
            )
        }
    }
}

tasks.jar {
    exclude("com/moloco/mcm/Main.class")
}