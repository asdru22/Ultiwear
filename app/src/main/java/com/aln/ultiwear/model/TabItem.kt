package com.aln.ultiwear.model

import androidx.compose.runtime.Composable

data class TabItem(
    val title: String,
    val icon: Int,
    val content: @Composable () -> Unit
)