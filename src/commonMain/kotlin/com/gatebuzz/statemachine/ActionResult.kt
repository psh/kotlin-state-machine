package com.gatebuzz.statemachine

interface ActionResult {
    fun fail()
    fun failAndExit()
}
