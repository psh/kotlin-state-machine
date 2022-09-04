package com.gatebuzz.statemachine

sealed class TestState : State {
    object StateA : TestState()
    object StateB : TestState()
    object StateC : TestState()
    object StateD : TestState()
}

sealed class SubgraphState: State {
    object StateOne: SubgraphState()
    object StateTwo: SubgraphState()
}
