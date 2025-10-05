package com.aln.ultiwear.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.compressAndUpload
import com.aln.ultiwear.model.Trade
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ManualTradeViewModel(
    private val wardrobeViewModel: WardrobeViewModel
) : ViewModel() {

    private val tag = "ManualTradeViewModel"
    val selectedGivenItems = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val receivedItems = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val showAddReceivedDialog = MutableStateFlow(false)
    val tradePhotoUri = MutableStateFlow<Uri?>(null)

    private val _isFinalizingTrade = MutableStateFlow(false)
    val isFinalizingTrade: StateFlow<Boolean> = _isFinalizingTrade.asStateFlow()

    fun setTradePhoto(uri: Uri?) {
        tradePhotoUri.value = uri
    }

    fun toggleGivenItem(item: WardrobeItem) {
        selectedGivenItems.value = if (selectedGivenItems.value.contains(item))
            selectedGivenItems.value - item
        else
            selectedGivenItems.value + item
    }

    fun setShowAddReceivedDialog(show: Boolean) {
        showAddReceivedDialog.value = show
    }

    fun addReceivedItem(
        front: Uri, back: Uri?, condition: Condition,
        size: Size, post: Boolean, tradeable: Boolean
    ) {
        // Construct WardrobeItem
        val newItem = WardrobeItem(
            id = UUID.randomUUID().toString(),
            frontImageUrl = front.toString(),
            backImageUrl = back?.toString(),
            conditionStr = condition.name,
            sizeStr = size.name,
            tradeable = tradeable,
            posted = post
        )
        receivedItems.value = receivedItems.value + newItem
        showAddReceivedDialog.value = false
    }

    fun finalizeTrade(context: Context, userBId: String) {
        viewModelScope.launch {
            _isFinalizingTrade.value = true
            try {
                // remove given items from wardrobe
                selectedGivenItems.value.forEach { item ->
                    wardrobeViewModel.deleteItem(item.id)
                }

                // upload received items to wardrobe
                val newItemIds = mutableListOf<String>()
                for (item in receivedItems.value) {
                    val frontUri = item.frontImageUrl.toUri()
                    val backUri = item.backImageUrl?.toUri()

                    val uploadedItem = wardrobeViewModel.uploadItemAndReturn(
                        frontUri = frontUri,
                        backUri = backUri,
                        condition = item.condition,
                        size = item.size,
                        post = item.posted,
                        tradeable = item.tradeable
                    )

                    uploadedItem?.id?.let { newItemIds.add(it) }
                }

                // upload trade record to Firestore
                val givenItemIds = selectedGivenItems.value.map { it.id }
                uploadTrade(
                    userAId = Firebase.auth.currentUser?.uid,
                    userBId = userBId,
                    userAItems = givenItemIds,
                    userBItems = newItemIds,
                    photoUri = tradePhotoUri.value
                )

                resetUI()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Trade successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(tag, "Error finalizing trade", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Trade failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isFinalizingTrade.value = false
            }
        }
    }

    private fun resetUI() {
        selectedGivenItems.value = emptyList()
        receivedItems.value = emptyList()
        tradePhotoUri.value = null
        showAddReceivedDialog.value = false
    }

    suspend fun uploadTrade(
        userAId: String?,
        userBId: String,
        userAItems: List<String>,
        userBItems: List<String>,
        photoUri: Uri?
    ) {
        val firestore = FirebaseFirestore.getInstance()

        var photoUrl: String? = null
        if (photoUri != null) {
            photoUrl = compressAndUpload(photoUri, "trade_photos/${UUID.randomUUID()}.webp")
        }

        val trade = Trade(
            id = firestore.collection("trades").document().id,
            userAId = userAId?: "unknown",
            userBId = userBId,
            userAItems = userAItems,
            userBItems = userBItems,
            photoUrl = photoUrl
        )

        firestore.collection("trades").document(trade.id).set(trade).await()
    }

}
