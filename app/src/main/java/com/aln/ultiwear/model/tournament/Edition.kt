package com.aln.ultiwear.model.tournament

import com.google.gson.annotations.SerializedName

data class Edition(
    @SerializedName("Id") val id: Int,
    @SerializedName("Lat") val lat: String?,
    @SerializedName("Lng") val lng: String?,
    @SerializedName("StartDate") val startDate: String?,
    @SerializedName("EndDate") val endDate: String?,
    @SerializedName("Country") val country: Country?
)