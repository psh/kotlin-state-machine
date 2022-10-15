![GitHub top language](https://img.shields.io/github/languages/top/psh/kotlin-state-machine)
[![ktlint](https://img.shields.io/badge/Kotlin%20Multiplatform-%E2%9D%A4-FF4081)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://app.travis-ci.com/psh/kotlin-state-machine.svg?branch=main)](https://app.travis-ci.com/github/psh/kotlin-state-machine)
[![Current Version](https://img.shields.io/badge/Version-0.5.0-1abc9c.svg)](https://shields.io/)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

# kotlin-state-machine

Some pointers to the layout of this project - 
* Code lives in `commonMain` as the state machine itself is a multiplatform library.
* Tests live in `commonTest` and should always pass for ALL platforms. 
* Examples use the library published to your `mavenLocal` repo to best simulate end-user usage scenarios.  Run the `publishToMavenLocal` gradle task before running the examples and all should be fine. 

## Core library

```kotlin
dependencies {
    implementation("com.gatebuzz:kotlin-state-machine:0.5.0")
}
```

## Compose Support

```kotlin
dependencies {
    implementation("com.gatebuzz:kotlin-state-machine:0.5.0")
    implementation("com.gatebuzz:kotlin-state-machine-compose:0.5.0")
}
```