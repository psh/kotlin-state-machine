package com.gatebuzz.statemachine.example.matter

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import com.gatebuzz.statemachine.graph
import kotlinx.coroutines.runBlocking

sealed class MatterState : State {
    object Solid : MatterState()
    object Liquid : MatterState()
    object Gas : MatterState()
}

sealed class MatterEvent : Event {
    object OnMelted : MatterEvent()
    object OnFrozen : MatterEvent()
    object OnVaporized : MatterEvent()
    object OnCondensed : MatterEvent()
}

val stateMachine = runBlocking {
    graph {
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
    }.start()
}
