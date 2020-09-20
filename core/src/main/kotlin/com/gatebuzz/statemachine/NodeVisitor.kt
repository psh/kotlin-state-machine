package com.gatebuzz.statemachine

fun interface NodeVisitor {
    fun accept(node: Node, trigger: Event?)
}
