package com.example.projectmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.projectmanager.screens.DashboardScreen
import com.example.projectmanager.screens.TermuxScreen
import com.example.projectmanager.screens.ApacheScreen
import com.example.projectmanager.screens.NginxScreen
import com.example.projectmanager.screens.PHPScreen
import com.example.projectmanager.screens.MyTemplatesScreen
import com.example.projectmanager.screens.DeploymentsScreen
import com.example.projectmanager.screens.SecurityScreen
import com.example.projectmanager.screens.SettingsScreen
// Placeholder screens
import com.example.projectmanager.screens.PostgreSQLScreen
import com.example.projectmanager.screens.MySQLScreen
import com.example.projectmanager.screens.StrapiScreen
import com.example.projectmanager.screens.SSHTerminalScreen

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.Termux.route) {
            TermuxScreen()
        }
        composable(Screen.Apache.route) {
            ApacheScreen()
        }
        composable(Screen.Nginx.route) {
            NginxScreen()
        }
        composable(Screen.PHP.route) {
            PHPScreen()
        }
        composable(Screen.PostgreSQL.route) {
            PostgreSQLScreen()
        }
        composable(Screen.MySQL.route) {
            MySQLScreen()
        }
        composable(Screen.Strapi.route) {
            StrapiScreen()
        }
        composable(Screen.MyTemplates.route) {
            MyTemplatesScreen()
        }
        composable(Screen.Deployments.route) {
            DeploymentsScreen()
        }
        composable(Screen.SSHTerminal.route) {
            SSHTerminalScreen()
        }
        composable(Screen.Security.route) {
            SecurityScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

