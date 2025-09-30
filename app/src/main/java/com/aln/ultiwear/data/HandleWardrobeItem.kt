package com.aln.ultiwear.data

import android.net.Uri
import android.util.Log
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID

const val tag = "HandleWardrobeItem"
suspend fun uploadImageAsync(uri: Uri, path: String): String? =
    suspendCancellableCoroutine { cont ->
        val storageRef = Firebase.storage.reference.child(path)
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { url ->
                        cont.resume(url.toString())
                        // suggested by android studio to avoid having to use
                        // a deprecated method
                        { cause, _, _ -> null?.let { it1 -> it1(cause) } }
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to get download URL: ${e.message}")
                        cont.resume(null)
                        { cause, _, _ -> null?.let { it1 -> it1(cause) } }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Upload failed: ${e.message}")
                cont.resume(null)
                { cause, _, _ -> null?.let { it(cause) } }
            }
    }

suspend fun uploadWardrobeItem(
    frontUri: Uri,
    backUri: Uri?,
    condition: Condition,
    size: Size,
    post: Boolean,
    tradeable: Boolean
): WardrobeItem? = coroutineScope {
    val id = UUID.randomUUID().toString()
    val ownerId = Firebase.auth.currentUser?.uid ?: "unknown"

    try {
        // upload front and back images in parallel
        val frontDeferred = async {
            uploadImageAsync(frontUri, "wardrobe/$id/front.jpg")
        }
        val backDeferred = backUri?.let {
            async {
                uploadImageAsync(it, "wardrobe/$id/back.jpg")
            }
        }

        val frontUrl = frontDeferred.await() ?: run {
            Log.e(tag, "Front image upload failed")
            return@coroutineScope null
        }
        val backUrl = backDeferred?.await()

        // save item to Firestore
        val item = WardrobeItem(
            id = id,
            owner = ownerId,
            conditionStr = condition.name,
            sizeStr = size.name,
            frontImageUrl = frontUrl,
            backImageUrl = backUrl,
            tradeable = tradeable,
            posted = post
        )

        Firebase.firestore.collection("wardrobe").document(id)
            .set(item)
            .await()
        Log.d(tag, "Wardrobe item uploaded")

        // create post if needed
        if (post) {
            makePost(item)
        }

        item
    } catch (e: Exception) {
        Log.e(tag, "Upload failed: ${e.message}")
        null
    }
}

// the listener updates the items when there are changes in the database
fun listenToWardrobeItems(
    userId: String,
    onItemsChanged: (List<WardrobeItem>) -> Unit
) {
    val firestore = Firebase.firestore
    firestore.collection("wardrobe")
        .whereEqualTo("owner", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("WardrobeScreen", "Error fetching items: ${error.message}")
                return@addSnapshotListener
            }
            val items = snapshot?.documents
                ?.mapNotNull { it.toObject(WardrobeItem::class.java) }
                ?: emptyList()
            onItemsChanged(items)
        }
}

suspend fun deleteWardrobeItem(id: String) {
    val firestore: FirebaseFirestore = Firebase.firestore
    try {

        val snapshot = firestore.collection("wardrobe")
            .document(id).get().await()
        val item = snapshot.toObject(WardrobeItem::class.java)

        // delete images
        item?.frontImageUrl?.let { url ->
            Firebase.storage.getReferenceFromUrl(url).delete().await()
            Log.d(tag, "Front image deleted")
        }
        item?.backImageUrl?.let { url ->
            Firebase.storage.getReferenceFromUrl(url).delete().await()
            Log.d(tag, "Back image deleted")
        }

        // delete wardrobe document
        firestore.collection("wardrobe")
            .document(id).delete().await()
        Log.d(tag, "Wardrobe item deleted")

        // delete associated post if present
        val postSnapshot = firestore.collection("posts")
            .whereEqualTo("wardrobeUid", id)
            .limit(1)
            .get()
            .await()

        if (postSnapshot.documents.isNotEmpty()) {
            postSnapshot.documents[0].reference.delete().await()
            Log.d(tag, "Post deleted")
        }

    } catch (e: Exception) {
        Log.e(tag, "Failed to delete item, images, or post", e)
        throw e
    }
}


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
