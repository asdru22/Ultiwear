package com.aln.ultiwear.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient { // singleton object declaration
    private const val URL = "https://api.ultical.com/v3/"

    val api: UlticalApi by lazy { // only created the first time the API is accessed
        Retrofit.Builder()
            .baseUrl(URL)
            // convert the json responses into kotlin objects
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            // create an implementation of the UlticalApi interface.
            .create(UlticalApi::class.java)
    }
}
