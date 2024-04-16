@file:Suppress("MemberVisibilityCanBePrivate")

package com.gatebuzz.verification

import com.gatebuzz.statemachine.ActionResult

class TestActionResult : ActionResult {
    var failCalled = false
    var failAndExitCalled = false

    override fun fail() {
        failCalled = true
    }

    override fun failAndExit() {
        failAndExitCalled = true
    }
}
