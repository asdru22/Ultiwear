package com.aln.ultiwear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.model.TabItem


@Composable
fun Footer(
    modifier: Modifier,
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = modifier
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            FooterButton(
                isSelected,
                tab,
                Modifier
                    .weight(1f)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent,
                        CircleShape
                    )
                    .clickable(
                        indication = null, // remove ripple rectangle
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(index) })
        }
    }
}

@Composable
fun FooterButton(
    isSelected: Boolean,
    tab: TabItem,
    modifier: Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = tab.icon),
            contentDescription = tab.title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
    }
}