package com.aln.ultiwear.data

import android.util.Log
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.collections.mapOf


private const val tag = "HandlePosts"

suspend fun makePost(item: WardrobeItem?) = try {
    val firestore: FirebaseFirestore = Firebase.firestore

    val postId = firestore.collection("posts").document().id
    val newPost = Post(
        wardrobeUid = item?.id ?: "none",
        likes = 0
    )

    firestore.collection("posts").document(postId)
        .set(newPost)
        .await()
    Log.d(tag, "Post created")
} catch (e: Exception) {
    Log.e("HandlePost", "Failed to create post", e)
}

suspend fun fetchPosts(limit: Long = 20): List<PostedWardrobeItem> = coroutineScope {
    val firestore = Firebase.firestore

    val snapshot = firestore.collection("wardrobe")
        .limit(limit)
        .get()
        .await()

    val wardrobeItems = snapshot.documents.mapNotNull { doc ->
        doc.toObject(WardrobeItem::class.java)?.copy(id = doc.id)
    }

    val deferredPosts = wardrobeItems.map { item ->
        async {
            val postQuery = firestore.collection("posts")
                .whereEqualTo("wardrobeUid", item.id)
                .limit(1)
                .get()
                .await()

            val post = postQuery.documents.firstOrNull()?.toObject(Post::class.java)

            PostedWardrobeItem(item, post)
        }
    }

    deferredPosts.awaitAll()
}

suspend fun toggleLike(postId: String, userId: String): Int {
    val firestore = Firebase.firestore
    val postRef = firestore.collection("posts").document(postId)
    val likeRef = postRef.collection("likes").document(userId)

    return firestore.runTransaction { tx ->
        val postSnap = tx.get(postRef)
        var likeCount = postSnap.getLong("likes") ?: 0L

        val likeSnap = tx.get(likeRef)
        if (likeSnap.exists()) {
            tx.delete(likeRef)
            likeCount--
        } else {
            tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            likeCount++
        }

        // create the document if it doesnt exist
        tx.set(postRef, mapOf("likes" to likeCount), SetOptions.merge())
        likeCount.toInt()
    }.await()
}
