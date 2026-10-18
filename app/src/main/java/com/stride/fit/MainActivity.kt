package com.stride.fit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stride.fit.sensors.StepService
import com.stride.fit.ui.MapViewModel
import com.stride.fit.ui.MindViewModel
import com.stride.fit.ui.PostureViewModel
import com.stride.fit.ui.ProfileViewModel
import com.stride.fit.ui.RunViewModel
import com.stride.fit.ui.TodayViewModel
import com.stride.fit.ui.TrainViewModel
import com.stride.fit.ui.screens.AccuracyScreen
import com.stride.fit.ui.screens.MindScreen
import com.stride.fit.ui.screens.OfflineMapsScreen
import com.stride.fit.ui.screens.OnboardingScreen
import com.stride.fit.ui.screens.PostureScreen
import com.stride.fit.ui.screens.ProfileScreen
import com.stride.fit.ui.screens.RunScreen
import com.stride.fit.ui.screens.TodayScreen
import com.stride.fit.ui.screens.TrainScreen
import com.stride.fit.ui.theme.StrideTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StrideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StrideRoot()
                }
            }
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val iconRes: Int
)

private val tabs = listOf(
    Tab("today", "Today", R.drawable.ic_tab_today),
    Tab("run", "Run", R.drawable.ic_tab_run),
    Tab("train", "Train", R.drawable.ic_tab_train),
    Tab("posture", "Posture", R.drawable.ic_tab_posture),
    Tab("you", "You", R.drawable.ic_tab_you)
)

@Composable
private fun StrideRoot() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val todayVm: TodayViewModel = viewModel()
    val runVm: RunViewModel = viewModel()
    val trainVm: TrainViewModel = viewModel()
    val postureVm: PostureViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()
    val mapVm: MapViewModel = viewModel()
    val mindVm: MindViewModel = viewModel()

    val profile by profileVm.profile.collectAsStateWithLifecycle()

    var hasActivityPermission by remember {
        mutableStateOf(hasPermission(context, activityPermission()))
    }
    var hasLocationPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    val startupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasActivityPermission = result[activityPermission()] ?: hasActivityPermission
        if (hasActivityPermission) StepService.start(context)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            result[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    fun requestLocation() {
        locationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Ask once, on first launch, then start counting immediately.
    LaunchedEffect(Unit) {
        val wanted = buildList {
            if (!hasActivityPermission) add(activityPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (wanted.isNotEmpty()) startupLauncher.launch(wanted.toTypedArray())
        else StepService.start(context)
    }

    if (!profile.onboarded) {
        OnboardingScreen(
            profile = profile,
            onFinish = { updated ->
                profileVm.updateProfile { updated.copy(onboarded = true) }
                profileVm.logWeight(updated.weightKg)
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // The app draws edge to edge and has no top bar, so Scaffold reports no top
        // padding at all. Without the status bar inset the first heading of every
        // screen slides underneath the clock.
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(LayoutDirection.Ltr),
            end = padding.calculateEndPadding(LayoutDirection.Ltr),
            top = maxOf(padding.calculateTopPadding(), statusBarTop) + 8.dp,
            bottom = padding.calculateBottomPadding() + 8.dp
        )

        NavHost(navController = navController, startDestination = "today") {
            composable("today") {
                TodayScreen(
                    vm = todayVm,
                    onOpenTraining = { navController.navigate("train") },
                    onOpenPosture = { navController.navigate("posture") },
                    onOpenRun = { navController.navigate("run") },
                    onOpenAccuracy = { navController.navigate("accuracy") },
                    contentPadding = contentPadding
                )
            }
            composable("run") {
                RunScreen(
                    vm = runVm,
                    tileStore = mapVm.tileStore,
                    hasLocationPermission = hasLocationPermission,
                    onRequestLocation = { requestLocation() },
                    onOpenMaps = { navController.navigate("maps") },
                    contentPadding = contentPadding
                )
            }
            composable("maps") {
                OfflineMapsScreen(
                    vm = mapVm,
                    onBack = { navController.popBackStack() },
                    onRequestLocation = { requestLocation() },
                    hasLocationPermission = hasLocationPermission,
                    contentPadding = contentPadding
                )
            }
            composable("train") {
                TrainScreen(
                    vm = trainVm,
                    // A plan day that is yoga or posture hands straight over to the
                    // guided player rather than making the user go and find it.
                    onOpenRoutine = { routineId ->
                        postureVm.startById(routineId)
                        navController.navigate("posture")
                    },
                    contentPadding = contentPadding
                )
            }
            composable("posture") {
                PostureScreen(
                    vm = postureVm,
                    onOpenMind = { navController.navigate("mind") },
                    contentPadding = contentPadding
                )
            }
            composable("mind") {
                MindScreen(
                    vm = mindVm,
                    onBack = { navController.popBackStack() },
                    contentPadding = contentPadding
                )
            }
            composable("you") {
                ProfileScreen(
                    vm = profileVm,
                    onOpenAccuracy = { navController.navigate("accuracy") },
                    contentPadding = contentPadding
                )
            }
            composable("accuracy") {
                AccuracyScreen(
                    vm = profileVm,
                    onBack = { navController.popBackStack() },
                    contentPadding = contentPadding
                )
            }
        }
    }
}

private fun activityPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        Manifest.permission.ACTIVITY_RECOGNITION
    else "com.google.android.gms.permission.ACTIVITY_RECOGNITION"

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
