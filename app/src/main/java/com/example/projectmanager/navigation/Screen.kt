package com.example.projectmanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Termux : Screen("termux", "Termux", Icons.Default.Android)
    object Apache : Screen("apache", "Apache", Icons.Default.Language)
    object Nginx : Screen("nginx", "Nginx", Icons.Default.Public)
    object PHP : Screen("php", "PHP", Icons.Default.Code)
    object PostgreSQL : Screen("postgresql", "PostgreSQL", Icons.Default.Storage)
    object MySQL : Screen("mysql", "MySQL", Icons.Default.DataObject)
    object Strapi : Screen("strapi", "Strapi", Icons.Default.Layers)
    object MyTemplates : Screen("templates", "My Templates", Icons.Default.ContentCopy)
    object Deployments : Screen("deployments", "Deployments", Icons.Default.Rocket)
    object SSHTerminal : Screen("ssh", "SSH Terminal", Icons.Default.Computer)
    object Security : Screen("security", "Security", Icons.Default.Security)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val menuItems = listOf(
    Screen.Dashboard,
    Screen.Termux,
    Screen.Apache,
    Screen.Nginx,
    Screen.PHP,
    Screen.PostgreSQL,
    Screen.MySQL,
    Screen.Strapi,
    Screen.MyTemplates,
    Screen.Deployments,
    Screen.SSHTerminal,
    Screen.Security,
    Screen.Settings
)

