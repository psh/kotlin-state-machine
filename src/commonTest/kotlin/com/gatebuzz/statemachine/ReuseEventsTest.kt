package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.ReuseEventsTest.ReusedEvents.Next
import com.gatebuzz.statemachine.ReuseEventsTest.ReusedStates.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ReuseEventsTest {

    @Test
    fun reuseEvents() = runTest {
        val testObject = graph {
            initialState(One)
            state(One) {
                on(Next) { transitionTo(Two) }
            }
            state(Two) {
                decision { _, event ->
                    return@decision if (event == Next) Next else null
                }
                on(Next) { transitionTo(Three) }
            }
            state(Three) {
                decision { _, event ->
                    return@decision if (event == Next) Next else null
                }
                on(Next) { transitionTo(Four) }
            }
            state(Four)
        }

        testObject.start()

        testObject.consume(Next)

        assertEquals(Four, testObject.currentState.id)
    }

    sealed class ReusedStates : State {
        data object One : ReusedStates()
        data object Two : ReusedStates()
        data object Three : ReusedStates()
        data object Four : ReusedStates()
    }

    sealed class ReusedEvents : Event {
        data object Next : ReusedEvents()
    }
}
