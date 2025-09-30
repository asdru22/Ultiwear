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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


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

    // fetch wardrobe items
    val wardrobeSnapshot = firestore.collection("wardrobe")
        .limit(limit)
        .get()
        .await()

    val wardrobeItems = wardrobeSnapshot.documents.mapNotNull { doc ->
        doc.toObject(WardrobeItem::class.java)?.copy(id = doc.id)
    }

    // fetch posts
    val postsSnapshot = firestore.collection("posts")
        .whereIn("wardrobeUid", wardrobeItems.map { it.id })
        .get()
        .await()

    val postsByWardrobeUid = postsSnapshot.documents.mapNotNull { doc ->
        doc.toObject(Post::class.java)?.let { it.wardrobeUid to it }
    }.toMap()

    // combine
    wardrobeItems.map { item ->
        PostedWardrobeItem(
            wardrobeItem = item,
            post = postsByWardrobeUid[item.id],
            wardrobeUid = item.id
        )
    }
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
