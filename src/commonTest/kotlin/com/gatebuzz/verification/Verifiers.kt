package com.gatebuzz.verification

var callCounter = 0

interface Verifier {
    var wasCalled: Boolean
    var callCount: Int
    val wasNotCalled: Boolean get() = !wasCalled

    infix fun wasCalledBefore(other: Verifier) = this.callCount < other.callCount
}
