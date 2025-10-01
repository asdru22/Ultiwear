package com.aln.ultiwear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun EventScreen(viewModel: EventViewModel, modifier: Modifier = Modifier) {
    val tournaments = viewModel.events
    val displayFormatter = DateTimeFormatter.ofPattern(
        "MMM dd yyyy",
        Locale.ENGLISH
    )

    Column(modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.event),
            action = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tournaments) { tournament ->
                TournamentCard(tournament = tournament, displayFormatter = displayFormatter)
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: TournamentUi,
    displayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(tournament.name, style = MaterialTheme.typography.titleMedium)

            val start = tournament.startDate?.let { ZonedDateTime.parse(it) }
            val end = tournament.endDate?.let { ZonedDateTime.parse(it) }

            if (start != null && end != null) {
                Text("${start.format(displayFormatter)} - ${end.format(displayFormatter)}")
            } else {
                Text("Dates: Unknown")
            }

            Text("Location: ${tournament.country ?: "Unknown"}")
        }
    }
}