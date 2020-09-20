package com.gatebuzz.statemachine

interface StateListener {
    fun onState(state: State)
}
