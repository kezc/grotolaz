plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "com.wojtek.holds"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.wojtek.holds.preprocessor.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

// Remove toolchain requirement, use default JVM
