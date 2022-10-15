---
sidebar_position: 5
title: Roadmap
---

* Version **0.1.0**
  * introduced the state machine and its declarative DSL for defining states and transitions
  * built as a traditional JVM library.
* Version **0.2.0**
  * Embraced Kotlin multiplatform
  * Use the Gradle version catalog to simplify the build
  * NOTE: this library is built against the **NEW** native memory module introduced in Kotlin 1.6.10
* Version **0.3.0**
  * Execute state transitions as coroutines
* Version **0.4.0**
  * Move the tests into `commonTest` so they can be run across all platforms
  * Fix reported bugs (thanks Jigar for reporting the issue, and steps to reproduce it)
* Version **0.5.0**
  * Full DSL / API review to ensure that it makes sense
  * Work through the callback and coroutines API to make sure that it also makes sense
  * Additional examples
* Version **0.6.0**
  * Overhaul of the documentation to reflect current state
* Version **1.0**
  * Publish the library to Maven Central

If you have feedback, bug reports or feature suggestions, please
drop an issue into the repo.  I also accept PRs!