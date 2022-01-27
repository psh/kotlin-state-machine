package com.gatebuzz.statemachine

sealed class TestEvents : Event {
    object TestEvent : TestEvents()
    object OtherTestEvent : TestEvents()
}
