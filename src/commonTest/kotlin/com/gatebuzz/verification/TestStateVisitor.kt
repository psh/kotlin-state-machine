package com.gatebuzz.verification

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.StateVisitor

class TestStateVisitor : StateVisitor, Verifier {
    override var wasCalled: Boolean = false
    override var callCount: Int = -1
    var state: State? = null
    var trigger: Event? = null

    override fun accept(state: State, trigger: Event?) {
        this.wasCalled = true
        this.callCount = ++callCounter
        this.state = state
        this.trigger = trigger
    }
}