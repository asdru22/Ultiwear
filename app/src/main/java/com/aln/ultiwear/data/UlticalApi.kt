package com.aln.ultiwear.data

import com.aln.ultiwear.model.tournament.ApiResponse
import retrofit2.http.GET

interface UlticalApi { // define contract for API endpoints
    @GET("event") // GET https://api.ultical.com/v3/event
    suspend fun getEvents(): ApiResponse
}