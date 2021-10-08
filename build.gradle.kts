buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.4.0")
    }
}

project.group = "com.gatebuzz.statemachine"
project.version = "0.1.0"
