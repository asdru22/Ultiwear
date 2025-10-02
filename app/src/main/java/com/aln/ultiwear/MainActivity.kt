package com.aln.ultiwear

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aln.ultiwear.data.GoogleAuthClient
import com.aln.ultiwear.model.TabItem
import com.aln.ultiwear.ui.screens.BrowseScreen
import com.aln.ultiwear.ui.screens.EventScreen
import com.aln.ultiwear.ui.screens.Footer
import com.aln.ultiwear.ui.screens.LoginScreen
import com.aln.ultiwear.ui.screens.SettingsScreen
import com.aln.ultiwear.ui.screens.WardrobeScreen
import com.aln.ultiwear.ui.theme.LocalBottomBarBackground
import com.aln.ultiwear.ui.theme.UltiwearTheme
import com.aln.ultiwear.viewModel.AuthViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val googleAuthClient by lazy { GoogleAuthClient(this) }
    // retrieve a ViewModel scoped to the Activity, ensures the viewModel
    // is retained over configuration changes
    // lazy creation guarantees that the viewModel is only created when it's
    // first used, not when the activity is created
    private val authViewModel: AuthViewModel by viewModels {
        // since normally viewModels() can only call a no-argument constructor,
        // but AuthViewModel needs a GoogleAuthClient parameter,
        // we give it a factory that knows how to build it
        viewModelFactory { // shorthand for ViewModelProvider.Factory
            initializer {
                AuthViewModel(googleAuthClient)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UltiwearTheme {
                val isSignedIn by authViewModel.isSignedIn.collectAsState()
                val hasInternet by authViewModel.hasInternet.collectAsState()

                LaunchedEffect(Unit) {
                    authViewModel.checkInternet(this@MainActivity)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // main app states
                    when {
                        !hasInternet -> {
                            NoInternetScreen(
                                onRetry = { authViewModel.checkInternet(this@MainActivity) }
                            )
                        }

                        isSignedIn -> {
                            AppWithBottomBar(onSignOut = { authViewModel.signOut() })
                        }

                        else -> {
                            LoginScreen(
                                onSignIn = { authViewModel.signIn() },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.no_internet))
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.try_again))
        }
    }
}


@Composable
fun AppWithBottomBar(

    onSignOut: () -> Unit
) {

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("Wardrobe", R.drawable.wardrobe) { WardrobeScreen() },
        TabItem("Social", R.drawable.social) { BrowseScreen() },
        TabItem("Events", R.drawable.events) { EventScreen() },
        TabItem("Trade", R.drawable.trade) { TradeScreen() },
        TabItem("Settings", R.drawable.settings) { SettingsScreen(onSignOut) }
    )

    Scaffold(
        bottomBar = {
            Footer(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(
                        color = LocalBottomBarBackground.current,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(vertical = 8.dp),
                tabs,
                selectedIndex,
                onTabSelected = { selectedIndex = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            tabs[selectedIndex].content()
        }
    }
}

@Composable
fun TradeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Trade") }
}