---
sidebar_position: 2
sidebar_label: Decision States
title: Decision States
---

![Even-Odd Diagram](/img/even-odd-state-diagram.png)

If you include a `decision` in a state definition, it will be executed in preference to the normal `onEnter`. The return
value from the decision lambda will be processed as if `consume()` had been called, with all the normal event handling /
transition rules. A return value `null` or other unhandled event wont cause a transition.

```kotlin
graph {
    initialState(StateA)

    state(StateA) { allows(StateB) }

    state(StateB) {
        decision { /* returns an event, or null */ }
        on(TestEvent) { transitionTo(StateA) }
        on(OtherTestEvent) { transitionTo(StateC) }
    }

    state(StateC)
}
```