package com.aln.ultiwear.data

import android.util.Log
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    val snapshot = firestore.collection("wardrobe")
        .limit(limit)
        .get()
        .await()

    val wardrobeItems = snapshot.documents.mapNotNull { doc ->
        doc.toObject(WardrobeItem::class.java)?.copy(id = doc.id)
    }

    // launch all post fetches concurrently
    val deferredPosts = wardrobeItems.map { item ->
        async {
            val postDoc = firestore.collection("posts")
                .document(item.id)
                .get()
                .await()
            val post = postDoc.toObject(Post::class.java)
            PostedWardrobeItem(item, post)
        }
    }

    deferredPosts.awaitAll()
}
