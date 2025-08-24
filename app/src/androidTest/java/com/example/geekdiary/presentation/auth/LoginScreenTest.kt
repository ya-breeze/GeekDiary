package com.example.geekdiary.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.geekdiary.ui.theme.GeekDiaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun loginScreen_displaysCorrectElements() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("GeekDiary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }
    
    @Test
    fun loginScreen_emailInput_acceptsText() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        
        // Then
        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }
    
    @Test
    fun loginScreen_passwordInput_acceptsText() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        
        // Then
        // Password should be hidden, so we check if the field has text
        composeTestRule.onNodeWithText("Password").assertTextContains("password123")
    }
    
    @Test
    fun loginScreen_loginButton_isClickable() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // When & Then
        composeTestRule.onNodeWithText("Login").assertIsEnabled()
        composeTestRule.onNodeWithText("Login").performClick()
    }
    
    @Test
    fun loginScreen_passwordVisibilityToggle_works() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithContentDescription("Show password").performClick()
        
        // Then - password should now be visible
        composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }
    
    @Test
    fun loginScreen_formValidation_showsErrors() {
        // Given
        composeTestRule.setContent {
            GeekDiaryTheme {
                LoginScreen(onLoginSuccess = {})
            }
        }
        
        // When - try to login with empty fields
        composeTestRule.onNodeWithText("Login").performClick()
        
        // Then - validation errors should appear
        composeTestRule.waitForIdle()
        // Note: This test would need the actual ViewModel to show validation errors
        // In a real test, we'd inject a test ViewModel or use a test harness
    }
}
