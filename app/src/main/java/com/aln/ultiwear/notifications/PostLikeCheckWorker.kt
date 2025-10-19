package com.aln.ultiwear.notifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aln.ultiwear.R
import com.aln.ultiwear.model.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class PostLikeCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    override suspend fun doWork(): Result {
        val currentUser = auth.currentUser ?: return Result.success()

        try {
            // get all posts made by the current user
            val postsSnapshot = firestore.collection("posts")
                .whereEqualTo("ownerId", currentUser.uid)
                .get()
                .await()

            for (doc in postsSnapshot.documents) {
                val post = doc.toObject(Post::class.java) ?: continue

                // check the if it has more than 5 likes and the notification wasn't yet sent
                if (post.likes >= 5 && !post.notification) {
                    sendNotification()

                    // update Firestore to set notification = true
                    doc.reference.update("notification", true)
                }
            }

            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun sendNotification() {

        val notification = NotificationCompat.Builder(
            applicationContext,
            "ultiwear_channel"
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Popular Post!")
            .setContentText("One of your posts just reached 5 likes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            // check for permission to send notifications
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1703, notification)
            }
        }
    }
}
