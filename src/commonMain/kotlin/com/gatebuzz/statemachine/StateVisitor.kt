package com.gatebuzz.statemachine

fun interface StateVisitor {
    fun accept(state: State, trigger: Event?)
}
