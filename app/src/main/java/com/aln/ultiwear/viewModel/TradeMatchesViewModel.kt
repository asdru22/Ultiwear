package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.model.TradeMatch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TradeMatchesViewModel(
    private val browseViewModel: BrowseViewModel,
    private val eventViewModel: EventViewModel
) : ViewModel() {

    private val tag = "TradeMatchesViewModel"
    val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _matches = mutableStateOf<List<TradeMatch>>(emptyList())
    val matches: MutableState<List<TradeMatch>> = _matches

    private val _incomingMatches = mutableStateOf<List<TradeMatch>>(emptyList())
    val incomingMatches: MutableState<List<TradeMatch>> = _incomingMatches

    private val _isLoading = mutableStateOf(true)
    val isLoading: MutableState<Boolean> = _isLoading


    fun loadMatchesWhenReady() {
        viewModelScope.launch {
            // wait for browseViewModel and eventViewModel to load
            while (browseViewModel.items.value.isEmpty() || eventViewModel.events.isEmpty()) {
                delay(100)
            }
            loadPostedMatches()
            loadInterestedMatches()
        }
    }

    fun loadPostedMatches() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // get current user's tradeable items
                val tradeableItems = browseViewModel.items.value
                    .filter { it.wardrobeItem.owner == currentUser.uid && it.wardrobeItem.tradeable }

                // get current user's attending tournaments
                val attendingTournamentIds = eventViewModel.attendances.filter { it.value }
                    .keys.toSet()
                if (tradeableItems.isEmpty() || attendingTournamentIds.isEmpty()) {
                    _matches.value = emptyList()
                    return@launch
                }

                val newMatches = mutableListOf<TradeMatch>()

                // preload all trade interests for all the current user's items in one query
                val tradeRef = firestore.collection("trade_interests")
                val allItemIds = tradeableItems.map { it.wardrobeItem.id }
                val tradeSnapshots = allItemIds.chunked(10)
                    .flatMap { chunk ->
                        // Firestore limits "in" queries to 10 items max
                        tradeRef.whereIn("itemId", chunk).get().await().documents
                    }

                // Map: itemId -> list of interestedUserIds
                val tradeMap = tradeSnapshots.groupBy(
                    keySelector = { it.getString("itemId") ?: "" },
                    valueTransform = { it.getString("interestedUserId") ?: "" }
                )

                // preload attendances for all interested users
                val interestedUserIds =
                    tradeSnapshots.mapNotNull { it.getString("interestedUserId") }.distinct()
                val attendanceMap =
                    // (userId, tournamentId) -> attending
                    mutableMapOf<Pair<String, Int>, Boolean>()

                // since firestore doesn't allow multiple documents in a single get easily
                // do a single fetch per user
                interestedUserIds.forEach { userId ->
                    val userTournaments = firestore
                        .collection("user_attendance")
                        .document(userId)
                        .collection("tournaments")
                        .get()
                        .await()
                    userTournaments.documents.forEach { doc ->
                        val tournamentId = doc.getLong("tournamentId")?.toInt()
                            ?: return@forEach
                        val attending = doc.getBoolean("attending") ?: false
                        attendanceMap[userId to tournamentId] = attending
                    }
                }

                // check matches
                tradeableItems.forEach { postedItem ->
                    val itemId = postedItem.wardrobeItem.id
                    val usersInterested = tradeMap[itemId] ?: emptyList()
                    if (usersInterested.isEmpty()) return@forEach

                    attendingTournamentIds.forEach { tournamentId ->
                        val matchCount = usersInterested.count { userId ->
                            attendanceMap[userId to tournamentId] == true
                        }
                        if (matchCount > 0) {
                            val tournament =
                                eventViewModel.events.firstOrNull { it.id == tournamentId }
                                    ?: return@forEach
                            newMatches.add(
                                TradeMatch(
                                    item = postedItem.wardrobeItem,
                                    tournament = tournament,
                                    matchCount = matchCount
                                )
                            )
                        }
                    }
                }

                _matches.value = newMatches

            } catch (e: Exception) {
                Log.e(tag, "Failed to load matches", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInterestedMatches() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // get all trade interests of current user (items they want to trade for)
                val interestSnapshot = firestore
                    .collection("trade_interests")
                    .whereEqualTo("interestedUserId", currentUser.uid)
                    .get()
                    .await()

                val interestedItemIds = interestSnapshot.documents
                    .mapNotNull { it.getString("itemId") }
                if (interestedItemIds.isEmpty()) {
                    _incomingMatches.value = emptyList()
                    return@launch
                }

                // fetch all items the user is interested in
                val allItems = browseViewModel.items.value
                    .filter { interestedItemIds.contains(it.wardrobeItem.id) }

                if (allItems.isEmpty()) {
                    _incomingMatches.value = emptyList()
                    return@launch
                }

                // fetch the current user's tournaments
                val myTournamentIds = eventViewModel
                    .attendances
                    .filter { it.value }
                    .keys
                    .toSet()
                if (myTournamentIds.isEmpty()) {
                    _incomingMatches.value = emptyList()
                    return@launch
                }

                val newMatches = mutableListOf<TradeMatch>()

                // fetch owners' tournaments
                val ownerIds = allItems.map { it.wardrobeItem.owner }.distinct()
                // (userId, tournamentId) -> attending
                val attendanceMap =
                    mutableMapOf<Pair<String, Int>, Boolean>()

                ownerIds.forEach { ownerId ->
                    val tournaments = firestore
                        .collection("user_attendance")
                        .document(ownerId)
                        .collection("tournaments")
                        .get()
                        .await()
                    tournaments.documents.forEach { doc ->
                        val tournamentId = doc.getLong("tournamentId")?.toInt()
                            ?: return@forEach
                        val attending = doc.getBoolean("attending") ?: false
                        attendanceMap[ownerId to tournamentId] = attending
                    }
                }

                // check for matches between current user and owners
                allItems.forEach { item ->
                    val ownerId = item.wardrobeItem.owner
                    myTournamentIds.forEach { tournamentId ->
                        val ownerAttends = attendanceMap[ownerId to tournamentId] == true
                        if (ownerAttends) {
                            val tournament =
                                eventViewModel.events.firstOrNull { it.id == tournamentId }
                                    ?: return@forEach
                            newMatches.add(
                                TradeMatch(
                                    item = item.wardrobeItem,
                                    tournament = tournament,
                                    matchCount = 1 // only count that owner attends
                                )
                            )
                        }
                    }
                }

                _incomingMatches.value = newMatches

            } catch (e: Exception) {
                Log.e(tag, "Failed to load incoming matches", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}