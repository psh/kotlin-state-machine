---
sidebar_position: 1
title: Getting Started
---
## Add Dependency
```kotlin title="build.gradle.kts"
dependencies {
    implementation("com.gatebuzz:kotlin-state-machine:0.5.0")
}
```

## Example State Machine

![Matter State Diagram](/img/matter-state-diagram.png)

Our state machine has three states:

```kotlin
sealed class MatterState : State {
    object Solid : MatterState()
    object Liquid : MatterState()
    object Gas : MatterState()
}
```

Allowing us to define a simple state machine for matter:

```kotlin
val stateMachine = graph {
    initialState(Solid)

    state(Solid) {
        allows(Liquid)
    }

    state(Liquid) {
        allows(Solid, Gas)
    }

    state(Gas) {
        allows(Liquid)
    }
}
```

Which can then be driven by calling `transitionTo()`:

```kotlin
stateMachine.start()

// sublimation not allowed - stays in Solid
stateMachine.transitionTo(Gas)

// melt the solid
stateMachine.transitionTo(Liquid)

// vaporize the liquid
stateMachine.transitionTo(Gas)
```

:::tip

If you prefer, you can have multiple `allows()` definitions rather than the comma-separated list

```kotlin
state(Liquid) {
    allows(Solid)
    allows(Gas)
}
```

:::

## Event Driven State Machine

Transitions between states can be triggered by events:

```kotlin
sealed class MatterEvent : Event {
    object OnMelted : MatterEvent()
    object OnFrozen : MatterEvent()
    object OnVaporized : MatterEvent()
    object OnCondensed : MatterEvent()
}
```

Allowing us to define an event-driven state machine that focuses more on the _edges_ between the nodes (the red arrows
in the state diagram):

```kotlin
val stateMachine = graph {
    initialState(Solid)

    state(Solid) {
        on(OnMelted) {
            transitionTo(Liquid)
        }
    }

    state(Liquid) {
        on(OnFrozen) {
            transitionTo(Solid)
        }
        on(OnVaporized) {
            transitionTo(Gas)
        }
    }

    state(Gas) {
        on(OnCondensed) {
            transitionTo(Liquid)
        }
    }
}
```

This event-driven state machine can then be driven by calling `consume()` or, `transitionTo()`

```kotlin
stateMachine.start()

// sublimation not allowed - stays in Solid
stateMachine.consume(OnVaporized)

// melt the solid
stateMachine.consume(OnMelted)

// vaporize the liquid
stateMachine.transitionTo(Gas)
```

:::info

Defining the state transition using events and `transitionTo()` statements implicitly set up the list of 
allowed transitions for a given state; there is no need to use `allows()` when using `transitionTo()`.

:::