package com.aln.ultiwear.view.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.view.shared.FrontImageSelectableCard
import com.aln.ultiwear.viewModel.TradeSessionViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun TradeSessionScreen(
    sessionId: String,
    viewModel: TradeSessionViewModel,
    wardrobeViewModel: WardrobeViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = Firebase.auth.currentUser?.uid

    val wardrobeItems by wardrobeViewModel.tradeableWardrobeItems.collectAsState()
    var pendingTrades by remember {
        mutableStateOf<List<Map<String, Any>>>(emptyList())
    }
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // listen to pending trades
    LaunchedEffect(sessionId) {
        Firebase.firestore.collection("trade_sessions")
            .document(sessionId)
            .collection("pending_trades")
            .addSnapshotListener { snapshot, _ ->
                pendingTrades = snapshot?.documents
                    ?.mapNotNull { it.data }
                    ?: emptyList()
            }
    }

    // get incoming and sending items
    val incomingItems = pendingTrades.filter { it["toUser"] == currentUserId }
    val sendingItems = wardrobeItems.filter { item ->
        pendingTrades.none { it["fromUser"] == currentUserId && it["itemId"] == item.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Trade Session", style = MaterialTheme.typography.headlineSmall)
        }

        item { Spacer(Modifier.height(12.dp)) }

        item { Text("Incoming Items", style = MaterialTheme.typography.titleMedium) }
        item { Spacer(Modifier.height(8.dp)) }

        // incoming items
        if (incomingItems.isEmpty()) {
            item { Text("No items received yet.") }
        } else {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incomingItems) { item ->
                        val imageUrl = item["frontImageUrl"]?.toString()
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Incoming Item",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Select Items to Send", style = MaterialTheme.typography.titleMedium) }
        item { Spacer(Modifier.height(8.dp)) }

        // items to send
        if (sendingItems.isEmpty()) {
            item { Text("No items available to send.") }
        } else {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sendingItems) { item ->
                        val isSelected = selectedItemIds.contains(item.id)
                        FrontImageSelectableCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                selectedItemIds = if (isSelected) selectedItemIds - item.id
                                else selectedItemIds + item.id
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        selectedItemIds.forEach { itemId ->
                            try {
                                viewModel.sendItemToOtherUser(sessionId, itemId)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        selectedItemIds = emptySet()
                    }
                },
                enabled = selectedItemIds.isNotEmpty()
            ) {
                Text("Send Selected Items")
            }
        }

        // complete trade
        item { Spacer(Modifier.height(16.dp)) }
        item {
            val hasItemsToFinalize =
                incomingItems.isNotEmpty() || pendingTrades.any { it["fromUser"] == currentUserId }
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            viewModel.finalizeTrade(sessionId)
                        } catch (e: Exception) {
                            Log.e("TradeSessionScreen", "Error finalizing trade", e)
                        }
                    }
                },
                enabled = hasItemsToFinalize
            ) {
                Text("Confirm Trade")
            }
        }
    }

}

