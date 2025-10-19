package com.aln.ultiwear

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aln.ultiwear.data.AuthHandler
import com.aln.ultiwear.notifications.TournamentCheckWorker
import com.aln.ultiwear.model.TabItem
import com.aln.ultiwear.notifications.PostLikeCheckWorker
import com.aln.ultiwear.ui.theme.LocalBottomBarBackground
import com.aln.ultiwear.ui.theme.UltiwearTheme
import com.aln.ultiwear.view.screens.BrowseScreen
import com.aln.ultiwear.view.screens.EventScreen
import com.aln.ultiwear.view.screens.Footer
import com.aln.ultiwear.view.screens.LoginScreen
import com.aln.ultiwear.view.screens.ProfileScreen
import com.aln.ultiwear.view.screens.TradeScreen
import com.aln.ultiwear.view.screens.WardrobeScreen
import com.aln.ultiwear.viewModel.AuthViewModel
import com.aln.ultiwear.viewModel.BrowseViewModel
import com.aln.ultiwear.viewModel.EventViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val authHandler by lazy { AuthHandler(this) }

    // retrieve a ViewModel scoped to the Activity, ensures the viewModel
    // is retained over configuration changes
    // lazy creation guarantees that the viewModel is only created when it's
    // first used, not when the activity is created
    private val authViewModel: AuthViewModel by viewModels {
        // since normally viewModels() can only call a no-argument constructor,
        // but AuthViewModel needs a AuthHandler parameter,
        // we give it a factory that knows how to build it
        viewModelFactory { // shorthand for ViewModelProvider.Factory
            initializer {
                AuthViewModel(authHandler)
            }
        }
    }

    private val wardrobeViewModel: WardrobeViewModel by viewModels()
    private val browseViewModel: BrowseViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissionsOnStartup()

        // gets the system service that handles notifications
        val manager = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        // create the channel the app will use for notifications
        manager.createNotificationChannel(
            NotificationChannel(
                "ultiwear_channel",
                "Tournament Days",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val tournamentCheckRequest = PeriodicWorkRequestBuilder<TournamentCheckWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "tournament_check",
                // don't start a new one if it's already running
                ExistingPeriodicWorkPolicy.KEEP,
            tournamentCheckRequest
            )

        val postLikeCheckRequest =
            PeriodicWorkRequestBuilder<PostLikeCheckWorker>(
                1,
                TimeUnit.HOURS
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "post_like_check",
            ExistingPeriodicWorkPolicy.KEEP,
            postLikeCheckRequest
        )

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

    // start requesting permissions
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermissionsOnStartup() {
        notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
    }

    // called after notification
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(CAMERA)
    }

    // called after camera permission
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(ACCESS_FINE_LOCATION)
    }

    // make the launchers  chain
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            requestCameraPermission() // next
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            requestLocationPermission() // next
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    @Composable
    fun AppWithBottomBar(
        onSignOut: () -> Unit
    ) {

        var selectedIndex by rememberSaveable { mutableIntStateOf(0) }


        val tabs = listOf(
            // wardrobe
            TabItem(
                "Wardrobe",
                R.drawable.wardrobe
            ) {
                WardrobeScreen(wardrobeViewModel)
            },
            // browse
            TabItem(
                "Browse",
                R.drawable.browse
            ) {
                BrowseScreen(
                    wardrobeViewModel = wardrobeViewModel,
                    browseViewModel = browseViewModel
                )
            },
            // events
            TabItem(
                "Events",
                R.drawable.events
            ) { EventScreen(viewModel = eventViewModel) },
            // trade
            TabItem(
                "Trade",
                R.drawable.trade
            ) {
                TradeScreen(
                    browseViewModel = browseViewModel,
                    eventViewModel = eventViewModel,
                    wardrobeViewModel = wardrobeViewModel
                )
            },
            // profile
            TabItem("Profile", R.drawable.profile) {
                ProfileScreen(
                    onSignOut = onSignOut
                )
            }
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
}