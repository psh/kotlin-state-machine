package com.gatebuzz.statemachine.example.matter

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import com.gatebuzz.statemachine.graph
import kotlinx.coroutines.runBlocking

const val ON_MELTED_MESSAGE = "I melted"
const val ON_FROZEN_MESSAGE = "I froze"
const val ON_VAPORIZED_MESSAGE = "I vaporized"
const val ON_CONDENSED_MESSAGE = "I condensed"

object TestLogger {
    private val logMessages: MutableList<String> = mutableListOf()

    val latest: String get() = logMessages.last()

    fun log(message: String) {
        logMessages.add(message)
    }
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

val stateMachine = runBlocking {
    graph {
        initialState(Solid)

        state(Solid) {
            on(OnMelted) {
                transitionTo(Liquid) { TestLogger.log(ON_MELTED_MESSAGE) }
            }
        }

        state(Liquid) {
            on(OnFrozen) {
                transitionTo(Solid) { TestLogger.log(ON_FROZEN_MESSAGE) }
            }

            on(OnVaporized) {
                transitionTo(Gas) { TestLogger.log(ON_VAPORIZED_MESSAGE) }
            }
        }

        state(Gas) {
            on(OnCondensed) {
                transitionTo(Liquid) { TestLogger.log(ON_CONDENSED_MESSAGE) }
            }
        }
    }.start()
}
