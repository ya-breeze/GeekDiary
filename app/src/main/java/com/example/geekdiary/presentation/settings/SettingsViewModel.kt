package com.example.geekdiary.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geekdiary.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "http://172.18.0.1:8080",
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentSettings()
    }
    
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            val currentUrl = settingsManager.getServerUrl()
            _uiState.value = _uiState.value.copy(
                serverUrl = currentUrl
            )
        }
    }
    
    fun updateServerUrl(newUrl: String) {
        // Clear previous messages
        _uiState.value = _uiState.value.copy(
            urlError = null,
            successMessage = null
        )
        
        // Validate URL
        val validationError = validateUrl(newUrl)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(
                urlError = validationError
            )
            return
        }
        
        // Save URL
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                settingsManager.setServerUrl(newUrl.trim())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    serverUrl = newUrl.trim(),
                    successMessage = "Server URL updated successfully. Please restart the app for changes to take effect."
                )
                
                // Clear success message after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.value = _uiState.value.copy(successMessage = null)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    urlError = "Failed to save settings: ${e.message}"
                )
            }
        }
    }
    
    private fun validateUrl(url: String): String? {
        return when {
            url.isBlank() -> "URL cannot be empty"
            !url.startsWith("http://") && !url.startsWith("https://") -> 
                "URL must start with http:// or https://"
            url.length < 10 -> "URL is too short"
            url.contains(" ") -> "URL cannot contain spaces"
            else -> null
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            urlError = null,
            successMessage = null
        )
    }
}
