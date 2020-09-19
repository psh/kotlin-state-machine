package com.gatebuzz.statemachine

interface ResultEmitter {
    fun success()
    fun failure()
    fun failAndExit()
}