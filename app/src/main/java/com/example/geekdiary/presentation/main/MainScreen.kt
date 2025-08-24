package com.example.geekdiary.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geekdiary.presentation.auth.AuthState
import com.example.geekdiary.presentation.auth.AuthViewModel
import com.example.geekdiary.ui.theme.GeekDiaryTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Trigger first-time sync when user logs in for the first time
    LaunchedEffect(authState) {
        val currentAuthState = authState
        if (currentAuthState is AuthState.Authenticated && currentAuthState.isFirstLogin) {
            viewModel.performFirstTimeSync()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeekDiary") },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sync message
            uiState.syncMessage?.let { syncMessage ->
                if (uiState.isSyncing) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                }
            }

            // Date Display Component
            DateDisplayComponent(
                currentDate = currentDate,
                onPreviousDay = { viewModel.navigateToPreviousDay() },
                onNextDay = { viewModel.navigateToNextDay() },
                onDateSelected = { date -> viewModel.navigateToDate(date) }
            )
            
            // Entry Display Component
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.entry != null -> {
                    EntryDisplayComponent(
                        entry = uiState.entry!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    EmptyStateComponent(
                        date = currentDate,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Error handling
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GeekDiaryTheme {
        MainScreen()
    }
}
