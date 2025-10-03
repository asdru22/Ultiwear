package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.PostHandler
import com.aln.ultiwear.model.PostedWardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class BrowseViewModel(val handler: PostHandler = PostHandler()) : ViewModel() {

    private val tag = "BrowseViewModel"
    private val _items = mutableStateOf<List<PostedWardrobeItem>>(emptyList())
    val items: State<List<PostedWardrobeItem>> = _items

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchPosts()
    }

    fun fetchPosts(limit: Long = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _items.value = handler.fetchPosts(limit)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching posts", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(
        postedItem: PostedWardrobeItem,
        onError: ((Exception) -> Unit)? = null
    ) {
        val currentUser = Firebase.auth.currentUser ?: return
        val wardrobeUid = postedItem.wardrobeUid

        viewModelScope.launch {
            try {
                val newLikes = handler.toggleLike(wardrobeUid, currentUser.uid)

                // update state
                _items.value = _items.value.map { item ->
                    if (item.wardrobeUid == wardrobeUid) {
                        item.copy(post = item.post?.copy(likes = newLikes))
                    } else item
                }
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    fun addTradeInterest(
        itemId: String,
        ownerId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val currentUser = Firebase.auth.currentUser ?: return

        val tradeRef = Firebase.firestore.collection("trade_interests")

        // prevent duplicate entries
        tradeRef
            .whereEqualTo("itemId", itemId)
            .whereEqualTo("interestedUserId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // user has not expressed interest yet, add document
                    val tradeDoc = hashMapOf(
                        "itemId" to itemId,
                        "ownerId" to ownerId,
                        "interestedUserId" to currentUser.uid,
                        "timestamp" to System.currentTimeMillis()
                    )
                    tradeRef.add(tradeDoc)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    // already expressed interest
                    Log.d("TradeInterest", "User has already expressed interest")
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    suspend fun hasUserExpressedInterest(itemId: String): Boolean {
        return handler.hasUserExpressedInterest(itemId)
    }
}

