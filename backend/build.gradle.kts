val ktor_version = "2.3.9"
val exposed_version = "0.41.1"
val sqlite_version = "3.41.2.1"
val logback_version = "1.5.3"
val kotest_version = "5.8.1"

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    application
}

group = "com.laundryflow"
version = "1.0.0"

application {
    mainClass.set("com.laundryflow.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Core & Server
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    
    // Serialization & Content Negotiation
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    
    // CORS Plugin
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    
    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    
    // SQLite JDBC Driver
    implementation("org.xerial:sqlite-jdbc:$sqlite_version")
    
    // Logging framework
    implementation("ch.qos.logback:logback-classic:$logback_version")
    
    // Testing capabilities (Ktor test, Kotest JVM runner & assertions)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
