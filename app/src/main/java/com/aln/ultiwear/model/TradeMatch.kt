package com.aln.ultiwear.model

import com.aln.ultiwear.model.tournament.TournamentUi

data class TradeMatch(
    val item: WardrobeItem,
    val tournament: TournamentUi,
    val matchCount: Int
)