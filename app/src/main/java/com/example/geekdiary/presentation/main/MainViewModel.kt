package com.example.geekdiary.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geekdiary.data.remote.NetworkResult
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = false,
    val entry: DiaryEntry? = null,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()
    
    init {
        loadEntryForCurrentDate()
    }
    
    fun navigateToPreviousDay() {
        val previousDate = _currentDate.value.minusDays(1)
        _currentDate.value = previousDate
        loadEntryForDate(previousDate)
    }
    
    fun navigateToNextDay() {
        val nextDate = _currentDate.value.plusDays(1)
        _currentDate.value = nextDate
        loadEntryForDate(nextDate)
    }
    
    fun navigateToDate(date: LocalDate) {
        _currentDate.value = date
        loadEntryForDate(date)
    }
    
    fun refreshEntry() {
        loadEntryForCurrentDate(forceRefresh = true)
    }
    
    private fun loadEntryForCurrentDate(forceRefresh: Boolean = false) {
        loadEntryForDate(_currentDate.value, forceRefresh)
    }
    
    private fun loadEntryForDate(date: LocalDate, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !forceRefresh,
                isRefreshing = forceRefresh,
                errorMessage = null
            )
            
            try {
                // First try to get from local database
                val localEntry = diaryRepository.getEntryByDate(date)
                
                if (localEntry != null && !forceRefresh) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        entry = localEntry
                    )
                } else {
                    // Try to fetch from remote
                    when (val result = diaryRepository.getEntriesFromRemote(date = date)) {
                        is NetworkResult.Success -> {
                            val remoteEntry = result.data.entries.firstOrNull()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                entry = remoteEntry ?: localEntry
                            )
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                entry = localEntry,
                                errorMessage = if (localEntry == null) result.exception.message else null
                            )
                        }
                        is NetworkResult.Loading -> {
                            // Keep current state
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
}
