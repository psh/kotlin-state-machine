---
sidebar_position: 3
sidebar_label: Long Running Transitions
title: Long Running Transitions
---

:::caution

This section of the library is in-flux and may change in later versions.  Feedback about the long-running
transition API is greatly appreciated!

:::

By default, state transitions are instantaneous and never fail. You can supply a block of code that will override that
behavior, allowing for long-running operations that have the option to succeed or fail.  The assumption is that the
action succeeds, so you only need to notify the state machine if there is a failure:

```kotlin
state(Solid) {
    on(OnMelted) {
        onEnter { }
        onExit { }
        transitionTo(Liquid)
        execute { result ->
            /* Do something that might take a while */

            if ( /* something went wrong */ ) {
                failure()
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
            /* Do something that might take a while */

            if ( /* some condition */ ) {
                result.success()
            } else {
                result.failure()
            }
        }
    }
}
``` 

By default, when a transition fails, the _exit_ block of the edge will not be called and the state machine will re-enter
the "from" state of the transition.

1. `Node` Solid OnExit
2. `Edge` Solid --> Liquid OnEnter
3. `Node` Solid OnEnter

An application might be tempted to show and hide a progress indicator (`onEnter` / `onExit`) while making a REST service
call (using `execute`)
but the lack of a call to the `onExit` when a transition fails would leave the progress indicator visible. In that case
the call to `failure()`
can be replaced with `failAndExit()` to ensure that the `onEnter` / `onExit` are still executed as a pair.

The execution block can be combined with the call to `transitionTo()` for a more concise syntax

```kotlin
state(Solid) {
    on(OnMelted) {
        transitionTo(Liquid) { result ->
            /* Do something that might take a while */

            if ( /* some condition */ ) {
                result.success()
            } else {
                result.failure()
            }
        }
    }
}
``` 

:::caution

Be aware that the state machine _will be left in limbo_ (the transition will never complete) if none of the
success or failure methods are called.

:::