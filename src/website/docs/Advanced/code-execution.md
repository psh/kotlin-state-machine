---
sidebar_position: 1
sidebar_label: Code Execution Triggers
title: Code Execution Triggers
---

![Matter State Diagram](/img/matter-state-diagram.png)

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

However, the state machine also has the concept of _edges_ between the nodes of the graph. It's possible to execute code
as we
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
