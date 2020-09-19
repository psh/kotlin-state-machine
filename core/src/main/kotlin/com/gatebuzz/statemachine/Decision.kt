package com.gatebuzz.statemachine

fun interface Decision {
    fun decide(node: Node): Event?
}