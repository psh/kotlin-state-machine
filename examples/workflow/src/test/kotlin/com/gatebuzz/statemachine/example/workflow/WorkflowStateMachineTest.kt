package com.gatebuzz.statemachine.example.workflow

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.example.workflow.WorkflowEvent.Next
import com.gatebuzz.statemachine.example.workflow.WorkflowEvent.Previous
import com.gatebuzz.statemachine.example.workflow.WorkflowState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class WorkflowStateMachineTest {
    @Test
    fun `initial state should be first page`() = runTest {
        stateMachine.start()

        assertEquals(Page1, stateMachine.currentState.id)
    }

    @Test
    fun `move to page 2`() = runTest {
        stateMachine.start(Dwelling(Page1))

        stateMachine.consume(Next)

        assertEquals(Page2, stateMachine.currentState.id)
    }

    @Test
    fun `page 1 has no previous`() = runTest {
        stateMachine.start(Dwelling(Page1))

        stateMachine.consume(Previous)

        assertEquals(Page1, stateMachine.currentState.id)
    }

    @Test
    fun `move to page 3 and back`() = runTest {
        stateMachine.start(Dwelling(Page2))

        stateMachine.consume(Next)
        assertEquals(Page3, stateMachine.currentState.id)

        stateMachine.consume(Previous)
        assertEquals(Page2, stateMachine.currentState.id)
    }

    @Test
    fun `move to page 4 and back`() = runTest {
        stateMachine.start(Dwelling(Page3))

        stateMachine.consume(Next)
        assertEquals(Page4, stateMachine.currentState.id)

        stateMachine.consume(Previous)
        assertEquals(Page3, stateMachine.currentState.id)

        stateMachine.consume(Previous)
        assertEquals(Page2, stateMachine.currentState.id)

        stateMachine.consume(Previous)
        assertEquals(Page1, stateMachine.currentState.id)
    }
}
