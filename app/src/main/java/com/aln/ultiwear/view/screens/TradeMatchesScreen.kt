package com.aln.ultiwear.view.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.aln.ultiwear.model.TradeMatch
import com.aln.ultiwear.viewModel.BrowseViewModel
import com.aln.ultiwear.viewModel.EventViewModel
import com.aln.ultiwear.viewModel.TradeMatchesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


@Composable
fun TradeMatchesScreen(
    browseViewModel: BrowseViewModel,
    eventViewModel: EventViewModel
) {
    val viewModel: TradeMatchesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TradeMatchesViewModel(
                    browseViewModel = browseViewModel,
                    eventViewModel = eventViewModel
                )
            }
        }
    )

    val matches by viewModel.matches
    val incomingMatches by viewModel.incomingMatches
    val isLoading by viewModel.isLoading

    var showIncoming by remember { mutableStateOf(false) }

    LaunchedEffect(browseViewModel.items.value, eventViewModel.events) {
        viewModel.loadMatchesWhenReady()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { showIncoming = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showIncoming) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Posted Items",
                    color = if (!showIncoming) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { showIncoming = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showIncoming) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Interested Items",
                    color = if (showIncoming) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val displayMatches = if (showIncoming) incomingMatches else matches

        if (isLoading || displayMatches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No trade matches found")
            }
        } else {
            MatchItems(
                displayMatches = displayMatches,
                showIncoming = showIncoming,
                viewModel = viewModel
            )

        }
    }
}

@Composable
fun MatchItems(
    displayMatches: List<TradeMatch>,
    showIncoming: Boolean,
    viewModel: TradeMatchesViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(displayMatches) { match ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        // Launch coroutine to send notifications
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val usersToNotify = if (!showIncoming) {
                                    // Posted items: notify all users interested in this item
                                    val snapshot = viewModel.firestore.collection("trade_interests")
                                        .whereEqualTo("itemId", match.item.id)
                                        .get().await()
                                    snapshot.documents.mapNotNull { it.getString("interestedUserId") }
                                } else {
                                    // Interested items: notify the owner
                                    listOf(match.item.owner)
                                }

                                if (usersToNotify.isNotEmpty()) {
                                    // Show a toast on main thread
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Notification sent to ${usersToNotify.size} user(s)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "No users to notify",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to send notifications",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                Log.e("TradeMatches", "Error sending notifications", e)
                            }
                        }
                    },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = match.item.frontImageUrl,
                        contentDescription = "Item Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(match.tournament.name, style = MaterialTheme.typography.bodyLarge)
                        if (!showIncoming) {
                            Text(
                                "Matches: ${match.matchCount}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}