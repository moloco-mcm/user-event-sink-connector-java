plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "com.moloco.mcm"
version = "0.1.0"
description = "Your library description"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Add your library dependencies here
    // Example:
    // implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.1")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.mockito:mockito-core:5.10.0")
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
                // Add other dependency Javadoc URLs as needed
            )
        }
    }
}

// Publishing Configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/moloco-mcm/user-event-sink-connector-java") // Change this

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("jongnam") // Change this
                        name.set("Jongnam Lee") // Change this
                        email.set("jongnam@moloco.com") // Change this
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/moloco-mcm/user-event-sink-connector-java.git") // Change this
                    developerConnection.set("scm:git:ssh://github.com/moloco-mcm/user-event-sink-connector-java.git") // Change this
                    url.set("https://github.com/moloco-mcm/user-event-sink-connector-java") // Change this
                }
            }
        }
    }
}

// Nexus Publishing Configuration
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

// Signing Configuration
signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

// Optional: Only sign when publishing to Maven Central
tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}