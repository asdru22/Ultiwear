package com.aln.ultiwear.data

import android.content.Context
import android.util.Log
import com.aln.ultiwear.model.tournament.TournamentUi
import com.aln.ultiwear.notifications.NotificationScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class EventHandler {

    private val tag = "EventHandler"

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    fun loadUserAttendances(attendances: MutableMap<Int, Boolean>) {
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

    fun setAttendance(
        context: Context,
        tournament: TournamentUi,
        attending: Boolean,
        attendances: MutableMap<Int, Boolean>
    ) {
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

        NotificationScheduler.scheduleDailyReminder(context)
        Log.i(tag, "Updated schedule")

        docRef.set(data)
            .addOnSuccessListener { Log.i(tag, "Attendance saved for $tournament") }
            .addOnFailureListener { Log.e(tag, "Failed to save attendance: ${it.message}") }
    }
}