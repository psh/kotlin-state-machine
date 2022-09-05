package com.gatebuzz.statemachine.example.workflow

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("State machine: $stateMachine")

    println("\nMove to next state")
    stateMachine.consume(WorkflowEvent.Next)

    println("State machine: $stateMachine")

    println("\n... and back again")
    stateMachine.consume(WorkflowEvent.Previous)

    println("State machine: $stateMachine")
}