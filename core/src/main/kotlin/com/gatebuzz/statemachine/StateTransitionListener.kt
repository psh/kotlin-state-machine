package com.gatebuzz.statemachine

interface StateTransitionListener {
    fun onStateTransition(state: MachineState)
}