package com.aln.ultiwear.model

data class Trade(
    val id: String = "",
    val userAId: String = "",
    val userBId: String = "",
    val userAItems: List<String> = emptyList(),
    val userBItems: List<String> = emptyList(),
    val photoUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)