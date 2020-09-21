package com.gatebuzz.statemachine.example.evenodd

import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.ResultEmitter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object RandomNumberRepository {
    var random: Int? = null

    fun getNumber(trigger: Event?, result: ResultEmitter) = RandomService.api.randomNumber().enqueue(
        object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    random = response.body()?.trim()?.toInt()
                    result.success(trigger)
                } else {
                    result.failure(trigger)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                t.printStackTrace()
                result.failure(trigger)
            }
        }
    )
}
