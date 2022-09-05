package com.gatebuzz.statemachine


typealias EdgeAction = suspend ActionResult.(Event?) -> Unit

interface ActionResult {
    fun fail()
    fun failAndExit()
}
