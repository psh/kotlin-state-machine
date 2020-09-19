package com.gatebuzz.statemachine

fun interface EdgeAction {
    fun execute(result: ResultEmitter)
}