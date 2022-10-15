---
sidebar_position: 3
title: Compose Support
---

```kotlin title="build.gradle.kts"
dependencies {
    implementation("com.gatebuzz:kotlin-state-machine:0.5.0")
    implementation("com.gatebuzz:kotlin-state-machine-compose:0.5.0")
}
```

The Compose support library adds three new methods to the Graph - firstly the
ability to collect type-safe state change events as Compose as _State_ for your
app

```kotlin title="Observing the graph as Compose state"
@Composable
@Preview
fun App() {
    val appState = appGraph.collectAsState<AppState>()

    MaterialTheme {
        when (appState.value) {
            AppState.Login -> Login()
            AppState.Home -> Home()
        }
    }
}
```

Then there are 2 methods (with default parameters for the coroutine dispatcher)
that make posting events or direct transitions a little easier

```kotlin title="Posting a state machine event"
@Composable
fun Login() {
    Button(onClick = {
        // fire off a login event to cause a state transition
        appGraph.postEvent(AppEvents.LoginEvent)
    }) {
        Text("Login now!")
    }
}
```

```kotlin title="posting a direct state machine transition"
@Composable
fun Home() {
    Button(onClick = {
        // transition directly to the login screen, don't use an event
        appGraph.postTransition(AppState.Login)
    }) {
        Text("Log me out, please.")
    }
}
```