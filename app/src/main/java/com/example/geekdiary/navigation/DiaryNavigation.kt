package com.example.geekdiary.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geekdiary.presentation.auth.AuthState
import com.example.geekdiary.presentation.auth.AuthViewModel
import com.example.geekdiary.presentation.auth.LoginScreen
import com.example.geekdiary.presentation.main.MainScreen
import com.example.geekdiary.presentation.settings.SettingsScreen

@Composable
fun DiaryNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = when (authState) {
            is AuthState.Authenticated -> DiaryDestinations.MAIN_ROUTE
            else -> DiaryDestinations.LOGIN_ROUTE
        }
    ) {
        composable(DiaryDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onLoginSuccess = {
                    authViewModel.onLoginSuccess()
                    navController.navigate(DiaryDestinations.MAIN_ROUTE) {
                        popUpTo(DiaryDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(DiaryDestinations.MAIN_ROUTE) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToSettings = {
                    navController.navigate(DiaryDestinations.SETTINGS_ROUTE)
                }
            )
        }

        composable(DiaryDestinations.SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
    
    // Handle authentication state changes
    when (authState) {
        is AuthState.Unauthenticated -> {
            if (navController.currentDestination?.route != DiaryDestinations.LOGIN_ROUTE) {
                navController.navigate(DiaryDestinations.LOGIN_ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        is AuthState.Authenticated -> {
            if (navController.currentDestination?.route == DiaryDestinations.LOGIN_ROUTE) {
                navController.navigate(DiaryDestinations.MAIN_ROUTE) {
                    popUpTo(DiaryDestinations.LOGIN_ROUTE) { inclusive = true }
                }
            }
        }
        else -> { /* Loading or Error states handled by individual screens */ }
    }
}

object DiaryDestinations {
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main"
    const val SETTINGS_ROUTE = "settings"
}
