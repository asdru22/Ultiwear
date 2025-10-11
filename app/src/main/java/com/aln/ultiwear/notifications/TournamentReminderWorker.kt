package com.aln.ultiwear.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aln.ultiwear.R
import com.aln.ultiwear.data.TournamentPrefs
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class TournamentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) { // worker that supports coroutines

    // everything in here runs on a background thread
    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = TournamentPrefs(context) // get the selected tournament
        // updates everytime theres a change in the flow
        val (name, date) = prefs.selectedTournament.first()

        if (name == null || date == null) return Result.success()

        // get the days left
        val tournamentDate = ZonedDateTime.parse(date)
        val daysLeft = ChronoUnit.DAYS.between(
            ZonedDateTime.now(),
            tournamentDate
        )

        if (daysLeft < 0) return Result.success() // for tournaments that have already passed

        // build the notification
        val notification = NotificationCompat.Builder(context, "ultiwear_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ultiwear Reminder")
            .setContentText("$daysLeft days until $name")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // show the notification
        with(NotificationManagerCompat.from(context)) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(4001, notification)
            }
        }

        return Result.success()
    }
}
