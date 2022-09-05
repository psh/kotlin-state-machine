package com.gatebuzz.statemachine.example.matterflow

import com.gatebuzz.statemachine.*
import com.gatebuzz.statemachine.example.matterflow.MatterEvent.*
import com.gatebuzz.statemachine.example.matterflow.MatterState.*
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
