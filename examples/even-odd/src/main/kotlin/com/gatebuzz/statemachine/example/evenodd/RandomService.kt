package com.gatebuzz.statemachine.example.evenodd

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

object RandomService {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl("https://www.random.org/integers/")
            .build()
    }

    val api: RandomApi by lazy {
        retrofit.create(RandomApi::class.java)
    }
}

interface RandomApi {
    @GET("?num=1&min=1&max=6&col=1&base=10&format=plain&rnd=new")
    suspend fun randomNumber(): String
}
