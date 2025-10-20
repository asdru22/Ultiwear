package com.aln.ultiwear.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
                    sendNotification(
                        context = applicationContext,
                        message = "One of your posts just reached 5 likes.",
                        title = "Popular Post!",
                        id = 1703
                    )

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
}
