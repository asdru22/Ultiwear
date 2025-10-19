package com.aln.ultiwear.viewModel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.ApiClient
import com.aln.ultiwear.data.EventHandler
import com.aln.ultiwear.model.tournament.TournamentUi
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class EventViewModel(private val handler: EventHandler = EventHandler()) : ViewModel() {
    val tag = "EventViewModel"
    val events = mutableStateListOf<TournamentUi>()
    val attendances = mutableStateMapOf<Int, Boolean>() // tournamentID -> attending

    init {  // when the viewModel is created, load the events and the attendances
        loadEvents()
        loadUserAttendances()
    }

    private fun loadEvents() {
        viewModelScope.launch { // launch a coroutine tied to the ViewModel’s lifecycle
            try {
                // returns an ApiResponse containing a list of Event objects
                val result = ApiClient.api.getEvents()
                events.clear() // clear any previous events

                val today = ZonedDateTime.now()

                result.data.forEach { event ->
                    event.editions.forEach { ed ->
                        // for each event and edition check if the start date
                        // is later than today
                        val start = ZonedDateTime.parse(ed.startDate)
                        if (start.isAfter(today)) {
                            events.add(
                                TournamentUi(
                                    id = ed.id,
                                    name = event.name,
                                    startDate = ed.startDate,
                                    endDate = ed.endDate,
                                    lat = ed.lat,
                                    lng = ed.lng,
                                    country = ed.country?.enShortName
                                )
                            )
                        }
                    }
                }
                Log.i(tag, "Loaded ${events.size} future tournaments")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "Failed: ${e.message}")
            }
        }
    }

    private fun loadUserAttendances() {
        handler.loadUserAttendances(attendances)
    }

    fun setAttendance(context: Context, tournament: TournamentUi, attending: Boolean) {
        handler.setAttendance(context, tournament, attending, attendances)
    }
}
