package com.gatebuzz.statemachine

sealed class TestEvents : Event {
    object TestEvent : TestEvents()
    object OtherTestEvent : TestEvents()
}

sealed class SubgraphEvents: Event {
    object Next: SubgraphEvents()
}