package com.aln.ultiwear.view.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aln.ultiwear.model.Trade
import com.aln.ultiwear.viewModel.TradesViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TradesHistoryDialog(
    wardrobeViewModel: WardrobeViewModel,
    tradesViewModel: TradesViewModel,
    onDismiss: () -> Unit
) {
    val trades by tradesViewModel.trades.collectAsState()
    val isLoading by tradesViewModel.isLoading.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trade History", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    trades.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trades found")
                    }

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trades) { trade ->
                            TradeItem(trade, wardrobeViewModel)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TradeItem(trade: Trade, wardrobeViewModel: WardrobeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val currentUserId = Firebase.auth.currentUser?.uid

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateString = dateFormat.format(Date(trade.timestamp))
            Text("$dateString:", style = MaterialTheme.typography.bodyMedium)

            trade.photoUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Trade Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Your Sent Items:", style = MaterialTheme.typography.bodySmall)
            Row {
                val yourItems = if (currentUserId == trade.userAId) trade.userAItems else trade.userBItems
                yourItems.forEach { itemId ->
                    val frontUrl = wardrobeViewModel.getFrontImage(itemId)
                    frontUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Sent Item",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Received Items:", style = MaterialTheme.typography.bodySmall)
            Row {
                val receivedItems = if (currentUserId == trade.userAId) trade.userBItems else trade.userAItems
                receivedItems.forEach { itemId ->
                    val frontUrl = wardrobeViewModel.getFrontImage(itemId)
                    frontUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Received Item",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

