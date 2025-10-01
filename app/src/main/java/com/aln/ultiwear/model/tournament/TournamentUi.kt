package com.aln.ultiwear.model.tournament

data class TournamentUi(
    val id: Int,
    val name: String,
    val startDate: String?,
    val endDate: String?,
    val lat: String?,
    val lng: String?,
    val country: String?
)