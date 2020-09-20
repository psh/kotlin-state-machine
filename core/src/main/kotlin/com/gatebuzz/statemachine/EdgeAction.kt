package com.gatebuzz.statemachine

fun interface EdgeAction {
    fun execute(trigger: Event?, result: ResultEmitter)
}
