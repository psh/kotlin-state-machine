package com.gatebuzz.statemachine.example.evenodd

import com.gatebuzz.statemachine.ActionResult

object RandomNumberRepository {
    var random: Int? = null

    suspend fun ActionResult.getNumber() {
        try {
            with(RandomService.api.randomNumber()) {
                random = trim().toInt()
            }
        } catch (e: Exception) {
            fail()
        }
    }
}
