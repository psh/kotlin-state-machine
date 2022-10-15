---
sidebar_position: 2
title: Observing State Changes
---

Suppose you have a basic state machine
```kotlin
sealed class MatterState : State {
    object Solid : MatterState()
    object Liquid : MatterState()
    object Gas : MatterState()
}

val stateMachine = graph {
    initialState(Solid)

    state(Solid) { ... }
    state(Liquid) { ... }
    state(Gas) { ... }
}
```

The graph you build is *observable* - you can observe either the states themselves as things change over time, 
or the lower-level state transitions (that includes edge traversal)

```kotlin
stateMachine.observeState().collect { state : State ->
    // called with each state that we land in eg, Solid or Gas
}

stateMachine.observeStateChanges().collect { machineState ->
    // called when dwelling on a particular node, 
    // eg, MachineState.Dwelling( Gas )
    //
    // or when traversing an edge of the graph,
    // eg, MachineState.Traversing( Liquid to Gas )
}
```

Or, the type-safe version 

```kotlin
stateMachine.observe<MatterState>() { state : MatterState ->
    // called with each state that we land in eg, Solid or Gas
}
```