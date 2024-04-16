package com.gatebuzz.statemachine

sealed class TestState : State {
    data object StateA : TestState()
    data object StateB : TestState()
    data object StateC : TestState()
}
