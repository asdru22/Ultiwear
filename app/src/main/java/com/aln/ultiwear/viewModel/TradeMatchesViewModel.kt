package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.TradeHandler
import com.aln.ultiwear.model.TradeMatch
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TradeMatchesViewModel(
    private val browseViewModel: BrowseViewModel,
    private val eventViewModel: EventViewModel,
    private val handler: TradeHandler = TradeHandler()
) : ViewModel() {

    private val tag = "TradeMatchesViewModel"

    private val _matches = mutableStateOf<List<TradeMatch>>(emptyList())
    val matches: MutableState<List<TradeMatch>> = _matches

    private val _incomingMatches = mutableStateOf<List<TradeMatch>>(emptyList())
    val incomingMatches: MutableState<List<TradeMatch>> = _incomingMatches

    private val _isLoading = mutableStateOf(true)
    val isLoading: MutableState<Boolean> = _isLoading


    fun loadMatchesWhenReady() {
        // start a coroutine tied to the ViewModel
        viewModelScope.launch {
            // observe multiple flows at once
            // and react when any of them emit a new value
            combine(
                // convert the states into snapshotFlows
                snapshotFlow { browseViewModel.items.value.isNotEmpty() },
                snapshotFlow { eventViewModel.events.isNotEmpty() }
                // merge the latest values from both into a single flow
                // indicates if both are ready
            ) { browseReady, eventsReady ->
                browseReady && eventsReady
            }
                // only pass the flows that are true
                .filter { it }
                // resume the coroutine after the first value that has passed
                // and stop observing the previous flows
                .first()

            loadPostedMatches()
            loadInterestedMatches()
        }
    }

    fun loadPostedMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tradeableItems = browseViewModel.items.value
                    .filter {
                        it.wardrobeItem.owner == Firebase.auth.currentUser?.uid
                                && it.wardrobeItem.tradeable
                    }

                val attendingTournamentIds = eventViewModel.attendances
                    .filter { it.value }
                    .keys.toSet()

                val allEvents = eventViewModel.events

                _matches.value = handler.fetchTradeMatches(
                    tradeableItems,
                    attendingTournamentIds,
                    allEvents
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to load matches", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInterestedMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val myAttendances = eventViewModel.attendances
                val browseItems = browseViewModel.items.value
                val allEvents = eventViewModel.events

                _incomingMatches.value = handler.fetchIncomingMatches(
                    browseItems,
                    myAttendances,
                    allEvents
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to load incoming matches", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}