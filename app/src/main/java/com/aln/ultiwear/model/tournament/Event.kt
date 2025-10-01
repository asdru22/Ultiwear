package com.aln.ultiwear.model.tournament

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String,
    @SerializedName("Edition") val editions: List<Edition>
)