package com.gatebuzz.statemachine.example.evenodd

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.example.evenodd.EvenOddEvent.*
import com.gatebuzz.statemachine.example.evenodd.EvenOddState.*
import com.gatebuzz.statemachine.example.evenodd.RandomNumberRepository.getNumber
import com.gatebuzz.statemachine.graph
import kotlinx.coroutines.runBlocking

sealed class EvenOddState : State {
    object Request : EvenOddState()
    object DecideResult : EvenOddState()
    object Even : EvenOddState()
    object Odd : EvenOddState()
}

sealed class EvenOddEvent : Event {
    object OnCallService : EvenOddEvent()
    object OnEven : EvenOddEvent()
    object OnOdd : EvenOddEvent()
}

val stateMachine = graph {
    initialState(Request)

    state(Request) {
        onEnter { _, _ -> println("Starting request flow") }
        onExit { _, event -> println("Exit Request: event = ${event.name()}") }

        on(OnCallService) {
            transitionTo(DecideResult) { getNumber() }
        }
    }

    state(DecideResult) {
        decision { _, event ->
            println("Decision: event = ${event.name()}")
            when (RandomNumberRepository.random!! % 2) {
                0 -> OnEven
                else -> OnOdd
            }
        }

        onExit { _, event -> println("Exit DecideResult: event = ${event.name()}") }

        on(OnEven) { transitionTo(Even) }
        on(OnOdd) { transitionTo(Odd) }
    }

    state(Even) {
        onEnter { _, _ -> println("Random result was: [[ Even ]]") }
    }

    state(Odd) {
        onEnter { _, _ -> println("Random result was: [[ Odd ]]") }
    }
}

private fun Event?.name() = this!!::class.java.simpleName

fun main() = runBlocking {
    stateMachine.start()
    stateMachine.consume(OnCallService)
}

