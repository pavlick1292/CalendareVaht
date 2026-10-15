package com.example.calendarevaht.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Главная", Icons.Filled.Home),
    SHIFTS("shifts", "Вахты", Icons.Filled.Work),
    SUMMARY("summary", "Сводка", Icons.Filled.CalendarMonth),
    MORE("more", "Ещё", Icons.Filled.MoreHoriz)
}