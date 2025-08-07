package com.example.dreamtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.compose.rememberNavController
import com.example.dreamtracker.screens.DreamDetailScreen
import com.example.dreamtracker.screens.DreamListScreen
import com.example.dreamtracker.screens.OnboardingScreen
import com.example.dreamtracker.screens.RecordDreamScreen
import com.example.dreamtracker.screens.SettingsScreen
import com.example.dreamtracker.screens.StatsScreen
import com.example.dreamtracker.settings.SettingsRepository
import com.example.dreamtracker.ui.theme.DreamTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var darkTheme by remember { mutableStateOf(isSystemInDarkTheme()) }
            val navController = rememberNavController()
            val settings = remember { SettingsRepository(applicationContext) }
            val scope = rememberCoroutineScope()

            DreamTrackerTheme(darkTheme = darkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("DreamTracker") },
                            actions = {
                                IconButton(onClick = { navController.navigate("stats") }) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_agenda),
                                        contentDescription = "Статистика"
                                    )
                                }
                                IconButton(onClick = { navController.navigate("settings") }) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_manage),
                                        contentDescription = "Настройки"
                                    )
                                }
                                IconButton(onClick = { darkTheme = !darkTheme }) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_day),
                                        contentDescription = "Переключить тему"
                                    )
                                }
                            }
                        )
                    }
                ) { _ ->
                    AppNavHost(navController = navController, onFinishOnboarding = {
                        scope.launch { settings.setOnboarded(true); navController.navigate("list") { popUpTo("onboarding") { inclusive = true } } }
                    })
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, onFinishOnboarding: () -> Unit) {
    val duration = 280
    AnimatedNavHost(
        navController = navController,
        startDestination = "list",
        enterTransition = {
            fadeIn(animationSpec = tween(duration)) + scaleIn(initialScale = 0.92f, animationSpec = tween(duration))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(duration)) + scaleOut(targetScale = 1.04f, animationSpec = tween(duration))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(duration)) + scaleIn(initialScale = 1.04f, animationSpec = tween(duration))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(duration)) + scaleOut(targetScale = 0.92f, animationSpec = tween(duration))
        }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onStartRecording = { navController.navigate("record") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("list") {
            DreamListScreen(
                onAddNew = { navController.navigate("record") },
                onOpen = { dreamId -> navController.navigate("detail/$dreamId") }
            )
        }
        composable("record") {
            RecordDreamScreen(onBack = { navController.popBackStack() })
        }
        composable("detail/{id}") { backStackEntry ->
            val idArg = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            DreamDetailScreen(dreamId = idArg, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("stats") {
            StatsScreen()
        }
    }
}