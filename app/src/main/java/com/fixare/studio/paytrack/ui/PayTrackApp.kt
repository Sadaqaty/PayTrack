package com.fixare.studio.paytrack.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fixare.studio.paytrack.ui.client.AddClientScreen
import com.fixare.studio.paytrack.ui.client.ClientDetailsScreen
import com.fixare.studio.paytrack.ui.client.ClientScreen
import com.fixare.studio.paytrack.ui.dashboard.DashboardScreen
import com.fixare.studio.paytrack.ui.log.LogScreen
import com.fixare.studio.paytrack.ui.settings.SettingsScreen
import com.fixare.studio.paytrack.ui.wallet.WalletScreen
import com.fixare.studio.paytrack.ui.welcome.WelcomeScreen
import com.fixare.studio.paytrack.ui.welcome.WelcomeViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Welcome : Screen("welcome", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Clients : Screen("clients", "Clients", Icons.Default.People)
    object Logs : Screen("logs", "Logs", Icons.Default.History)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddClient : Screen("add_client", "Add Client")
    object ClientDetails : Screen("client_details/{clientId}", "Client Details") {
        fun createRoute(clientId: Int) = "client_details/$clientId"
    }
}

@Composable
fun PayTrackApp(
    navController: NavHostController = rememberNavController(),
    welcomeViewModel: WelcomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val isFirstLaunch by welcomeViewModel.isFirstLaunch.collectAsState(initial = null)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Wait for isFirstLaunch to load
    if (isFirstLaunch == null) return

    val startDestination = if (isFirstLaunch == true) Screen.Welcome.route else Screen.Dashboard.route

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Clients,
        Screen.Wallet,
        Screen.Logs,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            // Hide bottom bar on detail screens AND Welcome screen
            val currentRoute = currentDestination?.route
            if (currentRoute != Screen.Welcome.route && currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut()
            }
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onContinue = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddClient = { navController.navigate(Screen.AddClient.route) },
                    onAddExpense = { 
                        navController.navigate(Screen.Wallet.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, 
                    onMarkPayment = { 
                        navController.navigate(Screen.Clients.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } 
                )
            }
            composable(Screen.Clients.route) {
                ClientScreen(
                    onAddClient = { navController.navigate(Screen.AddClient.route) },
                    onClientClick = { clientId ->
                        navController.navigate(Screen.ClientDetails.createRoute(clientId))
                    }
                )
            }
            composable(Screen.Wallet.route) {
                WalletScreen()
            }
            composable(Screen.Logs.route) {
                LogScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.AddClient.route) {
                AddClientScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ClientDetails.route,
                arguments = listOf(navArgument("clientId") { type = NavType.IntType })
            ) {
                ClientDetailsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
