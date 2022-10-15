---
sidebar_position: 4
title: Testing
---
## Starting The Machine At A Given State

![Matter State Diagram](/img/matter-state-diagram.png)

State machines are defined to be in an _inactive_ state when they are first defined (the large black dot on the state
diagram). A call to `start()` is required to make the initial transition into the defined _initialState_. Optionally a _machine state_
can be passed into the `start()` method to start the state machine at an arbitrary node in the graph. The state machine
allows either `Inactive` or `Dwelling` machine states to start and will throw an exception if you try to start
with `Traversing`.

```kotlin
@Test
fun `freezing should move us from liquid to solid`() {
    // Given
    stateMachine.start(Dwelling(Liquid))

    // When
    stateMachine.consume(OnFrozen)

    // Then
    assertEquals(Solid, stateMachine.currentState.id)
}
```

The `start()` method can be called at any time (and even multiple times) to reset the state machine to a given node in
the graph.