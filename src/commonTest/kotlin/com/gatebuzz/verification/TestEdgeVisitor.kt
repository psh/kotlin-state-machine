package com.gatebuzz.verification

import com.gatebuzz.statemachine.EdgeVisitor
import com.gatebuzz.statemachine.State

class TestEdgeVisitor : EdgeVisitor, Verifier {
    override var wasCalled: Boolean = false
    override var callCount: Int = -1
    var edge: Pair<State, State>? = null

    override fun accept(edge: Pair<State, State>) {
        this.wasCalled = true
        this.callCount = ++callCounter
        this.edge = edge
    }
}