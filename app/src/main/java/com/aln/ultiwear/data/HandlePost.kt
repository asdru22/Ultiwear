package com.aln.ultiwear.data


import android.util.Log
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


fun makePost(
    item: WardrobeItem?,
) {
    val tag = "HandlePost"
    val firestore: FirebaseFirestore = Firebase.firestore

    val postId = firestore.collection("posts").document().id
    val newPost = Post(
        wardrobeUid = item?.id ?: "none",
        likes = 0
    )
    firestore.collection("posts")
        .document(postId)
        .set(newPost)
        .addOnSuccessListener { Log.d(tag, "Post created") }
        .addOnFailureListener { e -> Log.e(tag, "Failed to create post", e) }
}

