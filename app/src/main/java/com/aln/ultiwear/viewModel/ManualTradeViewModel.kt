package com.aln.ultiwear.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.TradeHandler
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
import java.util.UUID

class ManualTradeViewModel(
    private val wardrobeViewModel: WardrobeViewModel,
    private val handler: TradeHandler = TradeHandler()
) : ViewModel() {

    private val tag = "ManualTradeViewModel"

    val selectedTournamentName = MutableStateFlow<String?>(null)
    val selectedGivenItems = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val receivedItems = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val showAddReceivedDialog = MutableStateFlow(false)
    val tradePhotoUri = MutableStateFlow<Uri?>(null)

    private val _isFinalizingTrade = MutableStateFlow(false)
    val isFinalizingTrade: StateFlow<Boolean> = _isFinalizingTrade.asStateFlow()

    fun setTradePhoto(uri: Uri?) {
        tradePhotoUri.value = uri
    }

    fun setSelectedTournament(name: String) {
        selectedTournamentName.value = name
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
        // make WardrobeItem
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
                    wardrobeViewModel.hideItem(item.id)
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
                    photoUri = tradePhotoUri.value,
                    tournamentName = selectedTournamentName.value
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
        selectedTournamentName.value = null
    }

    suspend fun uploadTrade(
        userAId: String?,
        userBId: String,
        userAItems: List<String>,
        userBItems: List<String>,
        photoUri: Uri?,
        tournamentName: String?
    ) {
        handler.uploadTrade(userAId, userBId, userAItems, userBItems, photoUri, tournamentName)
    }

}
