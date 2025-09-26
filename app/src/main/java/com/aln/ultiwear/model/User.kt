package com.aln.ultiwear.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null
)