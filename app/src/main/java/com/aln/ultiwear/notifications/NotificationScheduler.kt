package com.aln.ultiwear.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleDailyReminder(context: Context) {
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
            // otherwise, schedule it for 12pm today
            Duration.between(now, nextTime)
        }

        // get an instance of the WorkManager
        val workManager = WorkManager.getInstance(context)

        // make the worker execute once per day
        val workRequest = PeriodicWorkRequestBuilder<TournamentReminderWorker>(
            1, TimeUnit.DAYS
        )
            // used minutes for higher accuracy
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()

        // schedule this periodic job
        workManager.enqueueUniquePeriodicWork(
            "daily_tournament_reminder",
            // if a job with the same name exists, replace it
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.i(TAG, "Scheduled daily reminder at 12:00")
    }
}
