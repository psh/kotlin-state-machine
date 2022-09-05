package com.gatebuzz.statemachine.example.evenodd

import com.gatebuzz.statemachine.example.evenodd.EvenOddEvent.OnCallService
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    stateMachine.start()
    stateMachine.consume(OnCallService)
}
