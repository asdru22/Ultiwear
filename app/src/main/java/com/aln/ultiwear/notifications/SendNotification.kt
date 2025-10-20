package com.aln.ultiwear.notifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aln.ultiwear.MainActivity
import com.aln.ultiwear.R

fun sendNotification(
    context: Context,
    message: String,
    title: String,
    id: Int
) {

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
        .setContentTitle(title)
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
            notify(id, notification)
        }
    }
}