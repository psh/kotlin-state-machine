plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.compose")
}

group = "com.gatebuzz"
version = "0.5.0"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("com.gatebuzz:kotlin-state-machine-jvm:0.5.0")
            }
        }
        val jvmTest by getting
    }
}
