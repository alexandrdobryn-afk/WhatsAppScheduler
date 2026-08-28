package com.example.wascheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wascheduler.data.repository.AppTheme
import com.example.wascheduler.feature.diagnostics.DiagnosticsScreen
import com.example.wascheduler.feature.history.HistoryScreen
import com.example.wascheduler.feature.home.HomeScreen
import com.example.wascheduler.feature.home.HomeViewModel
import com.example.wascheduler.feature.onboarding.OnboardingScreen
import com.example.wascheduler.feature.onboarding.OnboardingViewModel
import com.example.wascheduler.feature.rule_editor.RuleEditorScreen
import com.example.wascheduler.feature.settings.SettingsScreen
import com.example.wascheduler.feature.settings.SettingsViewModel
import com.example.wascheduler.ui.theme.ThemeMode
import com.example.wascheduler.ui.theme.WaSchedulerTheme
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val RULE_EDITOR_NEW = "rule_editor"
    const val RULE_EDITOR_EDIT = "rule_editor/{ruleId}"
    fun ruleEditor(ruleId: Long) = "rule_editor/$ruleId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appTheme by settingsViewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
            val mode = when (appTheme) {
                AppTheme.SYSTEM -> ThemeMode.SYSTEM
                AppTheme.LIGHT -> ThemeMode.LIGHT
                AppTheme.DARK -> ThemeMode.DARK
            }
            WaSchedulerTheme(mode = mode) {
                RootScaffold()
            }
        }
    }
}

private data class BottomDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    BottomDestination(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
    BottomDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
private fun RootScaffold() {
    var onboardingChecked by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var onboardingDone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onboardingDone = onboardingViewModel.wasCompletedBefore()
        onboardingChecked = true
    }

    if (!onboardingChecked) return
    if (!onboardingDone) {
        OnboardingScreen(viewModel = onboardingViewModel, onContinue = { onboardingDone = true })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute !in setOf(Routes.RULE_EDITOR_NEW, Routes.RULE_EDITOR_EDIT)
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResourceCompat(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = vm,
                    onAddSchedule = { navController.navigate(Routes.RULE_EDITOR_NEW) },
                    onEditRule = { ruleId -> navController.navigate(Routes.ruleEditor(ruleId)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(viewModel = hiltViewModel())
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
                )
            }
            composable(Routes.DIAGNOSTICS) {
                DiagnosticsScreen(viewModel = hiltViewModel(), onBack = { navController.popBackStack() })
            }
            composable(Routes.RULE_EDITOR_NEW) {
                RuleEditorScreen(viewModel = hiltViewModel(), onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.RULE_EDITOR_EDIT,
                arguments = listOf(androidx.navigation.navArgument("ruleId") { type = androidx.navigation.NavType.LongType })
            ) {
                // ruleId is read from SavedStateHandle inside RuleEditorViewModel.
                RuleEditorScreen(viewModel = hiltViewModel(), onDone = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun stringResourceCompat(resId: Int): String = androidx.compose.ui.res.stringResource(id = resId)
