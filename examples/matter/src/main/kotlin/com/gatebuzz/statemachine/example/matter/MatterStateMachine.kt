package com.gatebuzz.statemachine.example.matter

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import com.gatebuzz.statemachine.graph

const val ON_MELTED_MESSAGE = "I melted"
const val ON_FROZEN_MESSAGE = "I froze"
const val ON_VAPORIZED_MESSAGE = "I vaporized"
const val ON_CONDENSED_MESSAGE = "I condensed"

interface Logger {
    fun log(message: String)
}

var logger: Logger = object : Logger {
    override fun log(message: String) = Unit
}

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

val stateMachine = graph {
    initialState(Solid)

    state(Solid) {
        on(OnMelted) {
            transitionTo(Liquid) {
                logger.log(ON_MELTED_MESSAGE)
                it.success()
            }
        }
    }

    state(Liquid) {
        on(OnFrozen) {
            transitionTo(Solid) {
                logger.log(ON_FROZEN_MESSAGE)
                it.success()
            }
        }

        on(OnVaporized) {
            transitionTo(Gas) {
                logger.log(ON_VAPORIZED_MESSAGE)
                it.success()
            }
        }
    }

    state(Gas) {
        on(OnCondensed) {
            transitionTo(Liquid) {
                logger.log(ON_CONDENSED_MESSAGE)
                it.success()
            }
        }
    }
}.start()