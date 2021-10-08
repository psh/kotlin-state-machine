import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    base
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("app.cash.turbine:turbine:0.6.1")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
}

base.archivesBaseName = "kotlin-state-machine"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}
