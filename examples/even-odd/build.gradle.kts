import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "com.gatebuzz.kotlin-state-machine.example"
version = "0.5.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.gatebuzz:kotlin-state-machine:0.5.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("com.gatebuzz.statemachine.example.evenodd.MainKt")
}
