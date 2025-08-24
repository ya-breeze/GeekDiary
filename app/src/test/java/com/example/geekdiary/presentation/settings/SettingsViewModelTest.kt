package com.example.geekdiary.presentation.settings

import com.example.geekdiary.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Mock
    private lateinit var settingsManager: SettingsManager

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads current server URL`() = runTest {
        // Given
        val expectedUrl = "http://test-server:8080"
        whenever(settingsManager.getServerUrl()).thenReturn(expectedUrl)

        // When
        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(expectedUrl, uiState.serverUrl)
        assertFalse(uiState.isLoading)
        assertNull(uiState.urlError)
        assertNull(uiState.successMessage)
    }

    @Test
    fun `updateServerUrl with valid URL saves successfully`() = runTest {
        // Given
        val initialUrl = "http://localhost:8080"
        val newUrl = "https://production-server.com"
        whenever(settingsManager.getServerUrl()).thenReturn(initialUrl)

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateServerUrl(newUrl)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsManager).setServerUrl(newUrl)
        val uiState = viewModel.uiState.first()
        assertEquals(newUrl, uiState.serverUrl)
        assertFalse(uiState.isLoading)
        assertNull(uiState.urlError)
        // Note: Success message might be cleared by the time we check due to the 5-second delay
        // So we don't assert on successMessage in this test
    }

    @Test
    fun `updateServerUrl with invalid URL shows error`() = runTest {
        // Given
        val initialUrl = "http://localhost:8080"
        val invalidUrl = "invalid-url"
        whenever(settingsManager.getServerUrl()).thenReturn(initialUrl)

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateServerUrl(invalidUrl)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(initialUrl, uiState.serverUrl) // Should not change
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.urlError)
        assertNull(uiState.successMessage)
    }

    @Test
    fun `updateServerUrl with empty URL shows error`() = runTest {
        // Given
        val initialUrl = "http://localhost:8080"
        whenever(settingsManager.getServerUrl()).thenReturn(initialUrl)

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateServerUrl("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(initialUrl, uiState.serverUrl)
        assertFalse(uiState.isLoading)
        assertEquals("URL cannot be empty", uiState.urlError)
        assertNull(uiState.successMessage)
    }

    @Test
    fun `clearMessages clears error and success messages`() = runTest {
        // Given
        val initialUrl = "http://localhost:8080"
        whenever(settingsManager.getServerUrl()).thenReturn(initialUrl)

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set an error first
        viewModel.updateServerUrl("")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearMessages()

        // Then
        val uiState = viewModel.uiState.first()
        assertNull(uiState.urlError)
        assertNull(uiState.successMessage)
    }
}
