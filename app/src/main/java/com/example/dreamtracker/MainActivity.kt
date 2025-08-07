package com.example.dreamtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dreamtracker.screens.DreamDetailScreen
import com.example.dreamtracker.screens.DreamListScreen
import com.example.dreamtracker.screens.RecordDreamScreen
import com.example.dreamtracker.screens.SettingsScreen
import com.example.dreamtracker.ui.theme.DreamTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var darkTheme by remember { mutableStateOf(isSystemInDarkTheme()) }
            DreamTrackerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("DreamTracker") },
                            actions = {
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
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "list") {
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
    }
}