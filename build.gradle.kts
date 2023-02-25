import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.3.1")
    }
}

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.endoqa"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")

    implementation("org.tinylog:tinylog-api-kotlin:2.6.0")
    implementation("org.tinylog:tinylog-impl:2.6.0")


    implementation("dev.cel:runtime:0.1.0")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks {
    val proguard = create<ProGuardTask>("proguard-main-jar") {
        group = "proguard"

        dependsOn(shadowJar)

        val inputFile = shadowJar.get().archiveFile.get().asFile.absolutePath

        injars(inputFile)
        outjars(inputFile.dropLast(".jar".length) + "-optimized.jar")

        configuration("main.proguard.pro")
        libraryjars(System.getProperty("java.home") + "/jmods")


    }
}