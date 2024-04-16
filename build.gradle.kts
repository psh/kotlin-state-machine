plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.binary.compatibility.validator)
    id("maven-publish")
}

val dependencyProvider = extensions.getByType<VersionCatalogsExtension>()
    .named("libs")
    .findLibrary("kotlin-state-machine")
    .get()

group = dependencyProvider.get().module.group
version = libs.versions.stateMachineVersion.get()

repositories {
    google()
    mavenCentral()
}

apiValidation {
    nonPublicMarkers.add("com.gatebuzz.statemachine.impl.InternalApi")
    ignoredClasses.add("com.gatebuzz.statemachine.impl.InternalApi")
}

kotlin {
    applyDefaultHierarchyTemplate()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.test)
            }
        }
        iosMain {}
        iosTest {}
        jvmMain {}
        jvmTest {}
        nativeMain {}
        nativeTest {}
    }
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports"))
}
