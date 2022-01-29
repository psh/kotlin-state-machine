package com.gatebuzz.verification

import com.gatebuzz.statemachine.Decision
import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State

class TestDecision(var result: Event?) : Decision, Verifier {
    override var wasCalled: Boolean = false
    override var callCount: Int = -1
    var state: State? = null
    var trigger: Event? = null

    override fun decide(state: State, trigger: Event?): Event? {
        this.wasCalled = true
        this.callCount = ++callCounter
        this.state = state
        this.trigger = trigger
        return result
    }
}
