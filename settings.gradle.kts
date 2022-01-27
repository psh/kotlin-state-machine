pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "kotlin-state-machine"

include(
    ":examples:even-odd",
    ":examples:matter",
    ":examples:matter-flow"
)
