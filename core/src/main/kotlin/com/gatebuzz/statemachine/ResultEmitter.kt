package com.gatebuzz.statemachine

interface ResultEmitter {
    fun success(trigger: Event?)
    fun failure(trigger: Event?)
    fun failAndExit(trigger: Event?)
}
