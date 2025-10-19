package com.aln.ultiwear.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.TradeHandler
import com.aln.ultiwear.model.Trade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TradesViewModel() : ViewModel() {
    private val handler: TradeHandler = TradeHandler()

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadTrades() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trades.value = handler.fetchUserTrades()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
