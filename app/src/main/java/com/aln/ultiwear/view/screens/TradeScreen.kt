package com.aln.ultiwear.view.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.R
import com.aln.ultiwear.viewModel.BrowseViewModel
import com.aln.ultiwear.viewModel.EventViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(browseViewModel: BrowseViewModel,
                eventViewModel: EventViewModel,
                wardrobeViewModel: WardrobeViewModel) {


    TradeMatchesScreen(
        browseViewModel = browseViewModel,
        eventViewModel = eventViewModel
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Matches", "Manual Trade", "Quick Trade")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(title = stringResource(R.string.trade))

        TradeScreenTabs(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )


        // tabs content
        when (selectedTab) {
            0 -> TradeMatchesScreen(browseViewModel, eventViewModel)
            1 -> ManualTradeScreen(wardrobeViewModel = wardrobeViewModel)
            2 -> QuickTradeScreen()
        }
    }
}

@Composable
fun TradeScreenTabs(tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTabSelected(index) },
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}