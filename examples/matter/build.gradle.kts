buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.jvm.kotlin.state.machine)

    implementation(libs.kotlin.coroutines.core)

    testImplementation(libs.bundles.jvm.test)
}
