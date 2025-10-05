package com.aln.ultiwear.model

data class TradeSessionState(
    val sessionId: String,
    val participants: List<String>,
    val isReady: Boolean
)
