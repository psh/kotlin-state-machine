package com.gatebuzz.statemachine

@Suppress("unused")
interface ResultEmitter {
    fun success(trigger: Event?)
    fun failure(trigger: Event?)
    fun failAndExit(trigger: Event?)
}
