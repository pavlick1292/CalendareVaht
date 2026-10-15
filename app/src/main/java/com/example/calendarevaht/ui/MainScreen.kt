package com.example.calendarevaht.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calendarevaht.ui.home.HomeScreen
import com.example.calendarevaht.ui.more.MoreScreen
import com.example.calendarevaht.ui.navigation.BottomNavItem
import com.example.calendarevaht.ui.shifts.ShiftsScreen
import com.example.calendarevaht.ui.summary.SummaryScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(BottomNavItem.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.HOME.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.HOME.route) { HomeScreen() }
            composable(BottomNavItem.SHIFTS.route) { ShiftsScreen() }
            composable(BottomNavItem.SUMMARY.route) { SummaryScreen() }
            composable(BottomNavItem.MORE.route) { MoreScreen() }
        }
    }
}