package com.example.calendarevaht.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calendarevaht.ui.calendar.CalendarScreen
import com.example.calendarevaht.ui.home.HomeScreen
import com.example.calendarevaht.ui.more.MoreScreen
import com.example.calendarevaht.ui.navigation.BottomNavItem
import com.example.calendarevaht.ui.onboarding.OnboardingScreen
import com.example.calendarevaht.ui.settings.SettingsScreen
import com.example.calendarevaht.ui.shifts.ShiftsScreen
import com.example.calendarevaht.ui.stats.StatsScreen
import com.example.calendarevaht.ui.summary.SummaryScreen

@Composable
fun MainScreen() {
    val onboardingVm: OnboardingViewModel = viewModel()
    val onboardingShown by onboardingVm.shown.collectAsState()

    when (onboardingShown) {
        null -> Box(Modifier.fillMaxSize())
        false -> OnboardingScreen(onFinish = { onboardingVm.markShown() })
        true -> MainContent()
    }
}

@Composable
private fun MainContent() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    val showBar = current != "summary_calendar"

    val barBg = MaterialTheme.colorScheme.surface
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onBackground
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = barBg) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = current == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) selectedColor else unselectedColor
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    color = if (selected) selectedColor else unselectedColor
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = selectedColor,
                                selectedTextColor = selectedColor,
                                indicatorColor = indicatorColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = BottomNavItem.HOME.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.HOME.route) {
                HomeScreen(onOpenSummaryCalendar = { nav.navigate("summary_calendar") })
            }
            composable(BottomNavItem.SHIFTS.route) { ShiftsScreen() }
            composable(BottomNavItem.SUMMARY.route) { SummaryScreen() }
            composable(BottomNavItem.MORE.route) {
                MoreScreen(
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenStats = { nav.navigate("stats") }
                )
            }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("stats") { StatsScreen(onBack = { nav.popBackStack() }) }
            composable("summary_calendar") { CalendarScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

