plugins {
    kotlin("multiplatform") version "1.6.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("maven-publish")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val dependencyProvider = versionCatalog.findDependency("kotlin-state-machine").get()
group = dependencyProvider.get().module.group
version = libs.versions.stateMachineVersion.get()

repositories {
    google()
    mavenCentral()
}

kotlin {
    ios()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "10"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlin.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.test)
            }
        }
        val iosMain by getting
        val iosTest by getting
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports"))
}
