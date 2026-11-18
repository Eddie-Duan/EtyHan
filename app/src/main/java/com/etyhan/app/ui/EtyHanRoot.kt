package com.etyhan.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.etyhan.app.EtyHanApp
import com.etyhan.app.ui.bank.BankScreen
import com.etyhan.app.ui.lookup.LookupScreen
import com.etyhan.app.ui.result.ResultScreen
import com.etyhan.app.ui.settings.SettingsScreen

private const val ROUTE_HOME = "home?q={q}"
private const val ROUTE_HOME_TARGET = "home"
private const val ROUTE_BANK = "bank"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_RESULT = "result/{id}"

@Composable
fun EtyHanRoot(initialQuery: String) {
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as EtyHanApp).container }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingInitial = remember { arrayOf(initialQuery) }

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route == ROUTE_HOME || route == ROUTE_BANK || route == ROUTE_SETTINGS

    val ttsMessage by container.speaker.message.collectAsStateWithLifecycle()
    LaunchedEffect(ttsMessage) {
        ttsMessage?.let {
            snackbarHostState.showSnackbar(it)
            container.speaker.dismissMessage()
        }
    }

    fun goTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = route == ROUTE_HOME,
                        onClick = { goTop(ROUTE_HOME_TARGET) },
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text("查询") },
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_BANK,
                        onClick = { goTop(ROUTE_BANK) },
                        icon = { Icon(Icons.Rounded.Bookmark, contentDescription = null) },
                        label = { Text("词库") },
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_SETTINGS,
                        onClick = { goTop(ROUTE_SETTINGS) },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("设置") },
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(inner),
        ) {
            composable(
                route = ROUTE_HOME,
                arguments = listOf(
                    navArgument("q") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val q = entry.arguments?.getString("q").orEmpty()
                LookupScreen(
                    container = container,
                    prefill = q.ifBlank { pendingInitial[0] },
                    onOpenResult = { id -> navController.navigate("result/$id") },
                    onOpenSettings = { goTop(ROUTE_SETTINGS) },
                )
            }
            composable(
                route = ROUTE_RESULT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ResultScreen(
                    container = container,
                    id = id,
                    onBack = { navController.popBackStack() },
                    onWordClick = { word ->
                        navController.navigate("home?q=${Uri.encode(word)}")
                    },
                    onOpenSettings = { goTop(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_BANK) {
                BankScreen(
                    container = container,
                    onOpenResult = { id -> navController.navigate("result/$id") },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(container = container)
            }
        }
    }
}
