package com.aln.ultiwear.data


import android.util.Log
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

suspend fun makePost(item: WardrobeItem?) = try {
    val tag = "HandlePost"
    val firestore: FirebaseFirestore = Firebase.firestore

    val postId = firestore.collection("posts").document().id
    val newPost = Post(
        wardrobeUid = item?.id ?: "none",
        likes = 0
    )

    firestore.collection("posts").document(postId)
        .set(newPost)
        .await() // suspend until complete
    Log.d(tag, "Post created")
} catch (e: Exception) {
    Log.e("HandlePost", "Failed to create post", e)
}

