package com.gatebuzz.statemachine

sealed class TestState : State {
    object StateA : TestState()
    object StateB : TestState()
    object StateC : TestState()
}