package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.model.Trade
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TradesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadTrades() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // fetch trades where user is either userA or userB
                val snapshotA = firestore.collection("trades")
                    .whereEqualTo("userAId", currentUserId)
                    .get()
                    .await()

                val snapshotB = firestore.collection("trades")
                    .whereEqualTo("userBId", currentUserId)
                    .get()
                    .await()

                // combine the snapshots
                val allDocuments = snapshotA.documents + snapshotB.documents

                val tradeList = allDocuments.mapNotNull { doc ->
                    try {
                        Trade(
                            id = doc.id,
                            userAId = doc.getString("userAId") ?: "",
                            userBId = doc.getString("userBId") ?: "",
                            userAItems = (doc.get("userAItems") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            userBItems = (doc.get("userBItems") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            photoUrl = doc.getString("photoUrl"),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("TradesViewModel", "Failed to load trades", e)
                        null
                    }
                }

                _trades.value = tradeList.sortedBy { it.timestamp }.reversed()
            } catch (e: Exception) {
                Log.e("TradesViewModel", "Failed to load trades", e)
                _trades.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
