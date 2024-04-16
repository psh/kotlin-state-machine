package com.gatebuzz.statemachine

sealed class TestEvents : Event {
    data object TestEvent : TestEvents()
    data object OtherTestEvent : TestEvents()
}
