package com.aln.ultiwear.model

data class User(
    val id: String,
    val email: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "email" to email
        )
    }
}