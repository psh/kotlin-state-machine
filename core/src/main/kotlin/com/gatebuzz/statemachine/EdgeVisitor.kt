package com.gatebuzz.statemachine

fun interface EdgeVisitor {
    fun accept(edge: Edge)
}