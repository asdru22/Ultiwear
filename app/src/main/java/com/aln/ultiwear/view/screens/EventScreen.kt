package com.aln.ultiwear.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.R
import com.aln.ultiwear.model.tournament.TournamentUi
import com.aln.ultiwear.viewModel.EventViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventScreen(modifier: Modifier = Modifier, viewModel: EventViewModel) {
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val tournaments = viewModel.events
    val displayFormatter = DateTimeFormatter.ofPattern(
        "MMM dd yyyy", Locale.ENGLISH
    )

    val filteredTournaments = if (searchQuery.isBlank()) tournaments
    else tournaments.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(modifier.fillMaxSize()) {
        TopBar(
            title = if (!searchActive) stringResource(R.string.event) else "",
            content = {
                if (searchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp),
                        placeholder = { Text("Search...") },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        trailingIcon = {
                            IconButton(onClick = {
                                searchActive = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                } else {
                    // open search mode
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Open search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        )

        if (filteredTournaments.isEmpty()) {
            // show loading indicator when no tournaments are available yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTournaments) { tournament ->
                    TournamentCard(
                        tournament = tournament,
                        displayFormatter = displayFormatter,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: TournamentUi,
    displayFormatter: DateTimeFormatter,
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val attending = viewModel.attendances[tournament.id] ?: false

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tournament.name, style = MaterialTheme.typography.titleMedium)

                // dates
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Dates",
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    val start = tournament.startDate?.let {
                        ZonedDateTime.parse(it)
                    }
                    val end = tournament.endDate?.let {
                        ZonedDateTime.parse(it)
                    }
                    if (start != null && end != null) {
                        Text("${start.format(displayFormatter)} - ${end.format(displayFormatter)}")
                    } else {
                        Text("Dates: Unknown")
                    }
                }

                // location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Text(tournament.country ?: "Unknown")
                }
            }

            Checkbox(
                checked = attending,
                onCheckedChange = { viewModel.setAttendance(tournament.id, it) }
            )
        }
    }
}
