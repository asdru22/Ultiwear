package com.aln.ultiwear.model

import com.google.firebase.Timestamp

data class PendingTrade(
    val fromUser: String = "",
    val toUser: String = "",
    val itemId: String = "",
    val frontImageUrl: String = "",
    val confirmedBySender: Boolean = false,
    val confirmedByReceiver: Boolean = false,
    val timestamp: Timestamp? = null
)
