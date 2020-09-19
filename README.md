# kotlin-state-machine

## Example State Machine
![Matter State Diagram](examples/matter/state-diagram.png)

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
        allows(Solid)
        allows(Gas)
    }

    state(Gas) {
        allows(Liquid)
    }
}
```
Note: you can either also have multiple `allows()` in a given state, or a single one with a comma separated list of allowed state transitions
 
```kotlin
state(Liquid) {
    allows(Solid, Gas)
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

Allowing us to define a simple state machine for matter:
```kotlin
val stateMachine = graph {
    initialState(Solid)

    state(Solid) {
        on(OnMelted) { transitionTo(Liquid) }
    }

    state(Liquid) {
        on(OnFrozen) { transitionTo(Solid) }

        on(OnVaporized) { transitionTo(Gas) }
    }

    state(Gas) {
        on(OnCondensed) { transitionTo(Liquid) }
    }
}
```
Note: defining the state transition using `transitionTo()` implicitly sets up the list of allowed transitions for a given state; there is no need to call `allows()` when using `transitionTo()`.

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

## Code Execution Triggers

The simplest execution triggers are _entry_ and _exit_ of our states:
```kotlin
state(Solid) {
    onEnter {
        // code executed each time we enter the Solid state 
    }   

    onExit {
        // code executed each time we leave the Solid state 
    }   

    on(OnMelted) { transitionTo(Liquid) }
}
```

However, the state machine also has the concept of _edges_ between the nodes of the graph. It's possible to execute code as we 
_enter_ and _exit_ the transition (that is, at the start and the end of the red lines in the state diagram):
```kotlin
// Event driven style
state(Solid) {
    onEnter { }   
    onExit { }   

    on(OnMelted) { 
        onEnter {
            // code executed each time we enter the 
            // transition state from Solid --> Liquid 
        }   

        onExit {
            // code executed each time we exit the 
            // transition state from Solid --> Liquid 
        }   

        transitionTo(Liquid) 
    }
}

// Non-event driven
state(Liquid) {
    onEnter { }   
    onExit { }   
    onTransitionTo(Gas) {
        onEnter { }   
        onExit { }   
    }
}
```

In this scenario, consuming the `OnMelted` event will trigger a transition which will execute the following steps: 
1. `Node` Solid OnExit
2. `Edge` Solid --> Liquid OnEnter
3. `Edge` Solid --> Liquid OnExit
4. `Node` Liquid OnEnter

## Conditional Transitions

State transitions can be cancelled.  In the long-form, for an event driven state machine
```kotlin
state(Solid) {
    on(OnMelted) { 
        onEnter { }   
        onExit { }   
        transitionTo(Liquid) 
        execute { result ->
            if ( /* some condition */ ) {
                result.success()
            } else {
                result.failure()
            }
        }        
    }
}
``` 
and for a non-event driven state machines :
```kotlin
state(Solid) {
    onTransitionTo(Liquid) { 
        onEnter { }   
        onExit { }   
        execute { result ->
            if ( /* some condition */ ) {
                result.success()
            } else {
                result.failure()
            }
        }        
    }
}
``` 

When a transition fails, the _exit_ block of the edge **is not called**, and the state machine will re-enter the _from_ state  
1. `Node` Solid OnExit
2. `Edge` Solid --> Liquid OnEnter
3. `Node` Solid OnEnter

The state machine _will be left in limbo_ (the transition will never complete) if neither the `success()` or `failure()` methods are called.

Note: the execution block can be combined with the call to `transitionTo()` for a more concise syntax
```kotlin
state(Solid) {
    on(OnMelted) { 
        transitionTo(Liquid) { result ->
            if ( /* some condition */ ) {
                result.success()
            } else {
                result.failure()
            }
        }        
    }
}
``` 

## Observing State Changes

Listeners can be added to observe the state transitions, or just the state changes themselves.
```kotlin
val stateMachine = graph {
    initialState(Solid)

    state(Solid) { }
    state(Liquid) { }
    state(Gas) { }

    onTransition {
        // called when dwelling on a particular node, or when
        // traversing an edge of the graph
    }

    onState {
        // called with each state we land in
    }
}
```