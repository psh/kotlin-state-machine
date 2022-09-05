package com.gatebuzz.statemachine.example.matterflow

import com.gatebuzz.statemachine.example.matterflow.MatterState.Gas
import com.gatebuzz.statemachine.example.matterflow.MatterState.Liquid
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("Statemachine: $stateMachine")

    stateMachine.transitionTo(Liquid)

    println("Statemachine: $stateMachine")

    stateMachine.transitionTo(Gas)

    println("Statemachine: $stateMachine")
}