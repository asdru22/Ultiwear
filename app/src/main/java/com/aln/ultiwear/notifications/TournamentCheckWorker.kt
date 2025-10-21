package com.aln.ultiwear.notifications

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aln.ultiwear.data.ApiClient
import com.aln.ultiwear.model.tournament.TournamentUi
import java.time.ZonedDateTime

// background worker scheduled via WorkManager
class TournamentCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // start the background task
    override suspend fun doWork(): Result {
        return try {
            // load tournament ids from previous call
            // open sharedPreferences XML file
            val prefs = applicationContext.getSharedPreferences(
                "tournaments_prefs",
                Context.MODE_PRIVATE
            )

            // get ids of tournaments from previous call
            // in the prefs, look for the key "known_ids"
            // if it's there, return the associated values (Set<String>)
            // otherwise return an empty set
            val savedIds = prefs.getStringSet(
                "known_ids",
                emptySet()
            ) ?: emptySet()

            // prepare to save new tournaments
            val newTournaments = mutableListOf<TournamentUi>()
            // the ids in this execution
            val currentIds = mutableSetOf<String>()
            val today = ZonedDateTime.now()

            // call the API to get the events
            val result = ApiClient.api.getEvents()

            // iterate over each event and edition
            result.data.forEach { event ->
                event.editions.forEach { ed ->
                    val start = ZonedDateTime.parse(ed.startDate)
                    if (start.isAfter(today)) {
                        // if its a future tournament, add it to the list
                        currentIds.add(ed.id.toString())
                        // if it wasn't in the saved IDs, add it to newTournaments
                        if (!savedIds.contains(ed.id.toString())) {
                            newTournaments.add(
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
            }

            // if the list is not empty, send a notification
            if (newTournaments.isNotEmpty()) {
                // join all tournament names into a single comma-separated string
                val tournamentNames = newTournaments.joinToString(", ") { it.name }

                // send one notification with all names
                sendNotification(
                    context = applicationContext,
                    message = "$tournamentNames.",
                    title = "New Tournaments Added!",
                    id = 1008
                )
            }

            // update prefs with the new ids
            prefs.edit { putStringSet("known_ids", currentIds) }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
