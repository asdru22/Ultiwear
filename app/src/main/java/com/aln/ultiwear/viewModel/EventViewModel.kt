package com.aln.ultiwear.viewModel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.ApiClient
import com.aln.ultiwear.data.TournamentPrefs
import com.aln.ultiwear.model.tournament.TournamentUi
import com.aln.ultiwear.notifications.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class EventViewModel : ViewModel() {
    val tag = "EventViewModel"
    val events = mutableStateListOf<TournamentUi>()
    val attendances = mutableStateMapOf<Int, Boolean>() // tournamentID -> attending

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("user_attendance")
            .document(uid)
            .collection("tournaments")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val tournamentId = doc.getLong("tournamentId")?.toInt()
                        ?: return@forEach
                    val attending = doc.getBoolean("attending") ?: false
                    attendances[tournamentId] = attending
                }
            }
            .addOnFailureListener {
                Log.e(tag, "Failed to load attendances: ${it.message}")
            }
    }

    fun setAttendance(context: Context, tournament: TournamentUi, attending: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        attendances[tournament.id] = attending

        val docRef = firestore.collection("user_attendance")
            .document(uid)
            .collection("tournaments")
            .document(tournament.id.toString())

        val data = mapOf(
            "tournamentId" to tournament.id, // tournamentId saved for ease of access
            "attending" to attending,
            "timestamp" to System.currentTimeMillis()
        )

        selectTournament(context, tournament)

        docRef.set(data)
            .addOnSuccessListener { Log.i(tag, "Attendance saved for $tournament") }
            .addOnFailureListener { Log.e(tag, "Failed to save attendance: ${it.message}") }
    }

    fun selectTournament(context: Context, tournament: TournamentUi) {
        viewModelScope.launch {
            val prefs = TournamentPrefs(context)
            prefs.saveTournament(tournament.name, tournament.startDate ?: return@launch)

            // get the list of tournaments the user is attending
            val attendingTournaments = events.filter { attendances[it.id] == true }

            // schedule reminder
            NotificationScheduler.scheduleDailyReminder(context, attendingTournaments.first())

            Log.i(tag, "Saved tournament ${tournament.name} and scheduled reminder")
        }
    }
}
