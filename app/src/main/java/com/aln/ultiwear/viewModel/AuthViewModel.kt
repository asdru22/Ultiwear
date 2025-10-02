package com.aln.ultiwear.viewModel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aln.ultiwear.data.GoogleAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    // mutable internal state
    private val _isSignedIn = MutableStateFlow(googleAuthClient.isSignedIn())
    // read-only state exposed to the UI
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _hasInternet = MutableStateFlow(false)
    val hasInternet: StateFlow<Boolean> = _hasInternet.asStateFlow()

    fun checkInternet(context: Context) {
        _hasInternet.value = isConnectedToInternet(context)
    }

    fun signIn() {
        // launches a coroutine in viewModelScope,
        // so it’s tied to the ViewModel lifecycle
        viewModelScope.launch {
            val success = googleAuthClient.signIn()
            if (success) _isSignedIn.value = true
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()
            _isSignedIn.value = false
        }
    }
}

private fun isConnectedToInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network)
        ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}