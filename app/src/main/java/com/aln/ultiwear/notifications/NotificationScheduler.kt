package com.aln.ultiwear.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.aln.ultiwear.model.tournament.TournamentUi
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    fun scheduleDailyReminder(context: Context, tournament: TournamentUi) {
        // get next trigger time
        val now = LocalDateTime.now()
        val nextTime = now.withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        // if its past the scheduled time, set it for 12pm the next day
        val initialDelay = if (now.isAfter(nextTime)) {
            Duration.between(now, nextTime.plusDays(1))
        } else {
            // otherwise, schedule it for 12pm today.
            Duration.between(now, nextTime)
        }

        // get an instance of the WorkManager
        val workManager = WorkManager.getInstance(context)

        // prepare input data for the worker
        val data = workDataOf(
            "tournamentName" to tournament.name,
            "tournamentDate" to (tournament.startDate ?: "")
        )

        // create a periodic work request that runs 1 day and starts after the delay
        val workRequest = PeriodicWorkRequestBuilder<TournamentReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(data)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()

        // put a period Work in queue
        workManager.enqueueUniquePeriodicWork(
            "tournament_reminder_${tournament.id}",
            // if another one with the same id is present, update the values
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.i(TAG, "Scheduled reminder for ${tournament.name}")

    }
}
