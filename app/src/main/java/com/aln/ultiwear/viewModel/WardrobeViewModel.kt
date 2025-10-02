package com.aln.ultiwear.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.deleteWardrobeItem
import com.aln.ultiwear.data.listenToWardrobeItems
import com.aln.ultiwear.data.makePost
import com.aln.ultiwear.data.uploadWardrobeItem
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WardrobeViewModel : ViewModel() {

    private val tag = "WardrobeViewModel"

    // mutable internal state & readonly state exposed to the UI
    private val _wardrobeItems = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val wardrobeItems: StateFlow<List<WardrobeItem>> = _wardrobeItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<WardrobeItem?>(null)
    val selectedItem: StateFlow<WardrobeItem?> = _selectedItem.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    init {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            listenToWardrobeItems(currentUserId) { items ->
                _wardrobeItems.value = items
            }
        }
    }

    fun setShowDialog(show: Boolean) {
        _showDialog.value = show
    }

    fun selectItem(item: WardrobeItem?) {
        _selectedItem.value = item
    }

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun uploadItem(
        frontUri: Uri,
        backUri: Uri? = null,
        condition: Condition,
        size: Size,
        post: Boolean,
        tradeable: Boolean
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadSuccess.value = false
            try {
                val item = withContext(Dispatchers.IO) {
                    uploadWardrobeItem(frontUri, backUri, condition, size, post, tradeable)
                }

                if (item == null) {
                    Log.e(tag, "Item upload failed")
                    return@launch
                }

                Log.d(tag, "Item uploaded: ${item.id}")

                if (post) {
                    try {
                        withContext(Dispatchers.IO) { makePost(item) }
                        Log.d(tag, "Post created for item ${item.id}")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to create post for ${item.id}", e)
                    }
                }

                _uploadSuccess.value = true
            } catch (e: Exception) {
                Log.e(tag, "Upload failed", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun resetUploadState() {
        _uploadSuccess.value = false
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                deleteWardrobeItem(id)
            } catch (e: Exception) {
                Log.e(tag, "Failed to delete item", e)
            }
        }
    }
}
