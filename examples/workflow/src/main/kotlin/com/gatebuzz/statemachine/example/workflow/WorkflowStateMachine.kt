package com.gatebuzz.statemachine.example.workflow

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.example.workflow.WorkflowEvent.Next
import com.gatebuzz.statemachine.example.workflow.WorkflowEvent.Previous
import com.gatebuzz.statemachine.example.workflow.WorkflowState.*
import com.gatebuzz.statemachine.graph
import kotlinx.coroutines.runBlocking

sealed class WorkflowState : State {
    object Page1 : WorkflowState()
    object Page2 : WorkflowState()
    object Page3 : WorkflowState()
    object Page4 : WorkflowState()
}

sealed class WorkflowEvent : Event {
    object Next : WorkflowEvent()
    object Previous : WorkflowEvent()
}

val stateMachine = runBlocking {
    graph {
        initialState(Page1)

        state(Page1) {
            on(Next) { transitionTo(Page2) }
        }

        state(Page2) {
            on(Previous) { transitionTo(Page1) }
            on(Next) { transitionTo(Page3) }
        }

        state(Page3) {
            on(Previous) { transitionTo(Page2) }
            on(Next) { transitionTo(Page4) }
        }

        state(Page4) {
            on(Previous) { transitionTo(Page3) }
        }

    }.start()
}
