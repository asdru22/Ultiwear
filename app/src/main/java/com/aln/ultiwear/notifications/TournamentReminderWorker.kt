package com.aln.ultiwear.notifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aln.ultiwear.MainActivity
import com.aln.ultiwear.R
import com.aln.ultiwear.data.ApiClient
import com.aln.ultiwear.model.tournament.TournamentUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class TournamentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "TournamentReminderWorker"

    // i am not using a view model because they
    // exist only tied to the lifecycle of the activity
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val context = applicationContext
        val uid = auth.currentUser?.uid ?: return Result.success()

        try {
            // load user attendances from Firestore
            val attendanceSnapshot = firestore
                .collection("user_attendance")
                .document(uid)
                .collection("tournaments")
                .get()
                .await()

            val attendances = attendanceSnapshot.documents.associate { doc ->
                val tournamentId = doc.getLong("tournamentId")?.toInt() ?: -1
                val attending = doc.getBoolean("attending") ?: false
                tournamentId to attending
            }

            // load events from API
            val result = ApiClient.api.getEvents()
            val today = ZonedDateTime.now()

            // iterate over every element in result.data
            // and flatten it into a list of tournaments by edition
            val events = result.data.flatMap { event ->
                // iterate over each event and then over all its editions
                event.editions.mapNotNull { ed ->
                    val start = try {
                        // try to parse the date
                        ZonedDateTime.parse(ed.startDate)
                    } catch (e: Exception) {
                        // if it fails, set the start date to null
                        Log.e(tag, "Error", e)
                        null
                        // if start date is null,
                        // stop this iteration of mapNotNull and skip this edition
                        // (because the map skips any transformations that return null)
                    } ?: return@mapNotNull null

                    // convert to object the tournaments that start after today
                    // and implicitly add it to the flatmap if it starts after today
                    if (start.isAfter(today)) {
                        TournamentUi(
                            id = ed.id,
                            name = event.name,
                            startDate = ed.startDate,
                            endDate = ed.endDate,
                            lat = ed.lat,
                            lng = ed.lng,
                            country = ed.country?.enShortName
                        )
                    } else null
                }
            }

            // in the attendances map, check if the id for the filtered tournament exists
            val attendingTournaments = events.filter {
                attendances[it.id] == true
            }

            // sort by nearest start date and take top 3
            val now = ZonedDateTime.now()


            // iterate over every tournament in attendingTournaments
            // and make a new mapNotNull
            val upcoming = attendingTournaments.mapNotNull { t ->
                val start = try {
                    // attempt to parse
                    ZonedDateTime.parse(t.startDate)
                } catch (e: Exception) {
                    Log.e(tag, "Parse failed", e)
                    null
                }
                // if parsing works, create a pair between t and the days until te tournament
                start?.let {
                    t to ChronoUnit.DAYS.between(
                        now,
                        it
                    )
                }
                // check the second term of the pair to be >0
            }.filter { it.second >= 0 }
                // sort by the second term
                .sortedBy { it.second }
                // and get the first 3
                .take(3)

            // if there are no upcoming tournaments, finish the worker successfully
            // and do nothing else
            if (upcoming.isEmpty()) {
                Log.i(tag, "No upcoming tournaments to notify")
                return Result.success()
            }

            // otherwise, create a notification

            // make notification content
            val message = upcoming.joinToString("\n")
            { (t, days) ->
                "• ${t.name}: in $days days"
            }

            // create the intent to launch the app when the notification is clicked
            val launchIntent = Intent(
                context,
                MainActivity::class.java
            ).apply {
                // launch the app if its closed, or restart it if its open
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // wrap the Intent in a PendingIntent, so that it can be executed later
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                // update an existing pending intent if it already exists
                // make the pending intent immutable (required for security)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // build the notification
            val notification = NotificationCompat.Builder(
                context,
                "ultiwear_channel"
            )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Upcoming Tournaments")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                // delete the notification when clicked
                .setAutoCancel(true)
                .build()

            // show the notification
            with(NotificationManagerCompat.from(context)) {
                // check for permission to send notifications
                if (ActivityCompat.checkSelfPermission(
                        context,
                        POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(1234, notification)
                }
            }

            Log.i(tag, "Notification sent for upcoming tournaments")

            // finish the worker
            return Result.success()

        } catch (e: Exception) {
            Log.e(tag, "Error in reminder worker: ${e.message}")
            e.printStackTrace()
            return Result.failure()
        }
    }
}

