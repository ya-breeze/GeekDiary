package com.example.geekdiary.presentation.main

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.ui.theme.GeekDiaryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun entryDisplayComponent_displaysEntryCorrectly() {
        // Given
        val testEntry = DiaryEntry(
            date = LocalDate.of(2024, 1, 15),
            title = "Test Entry",
            body = "This is a test diary entry with some content to display.",
            tags = listOf("test", "example")
        )
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                EntryDisplayComponent(entry = testEntry)
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Test Entry").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a test diary entry with some content to display.").assertIsDisplayed()
        composeTestRule.onNodeWithText("test").assertIsDisplayed()
        composeTestRule.onNodeWithText("example").assertIsDisplayed()
    }
    
    @Test
    fun entryDisplayComponent_expandsContent() {
        // Given
        val longContent = "This is a very long diary entry content that should be truncated initially and then expanded when the user clicks the show more button. ".repeat(10)
        val testEntry = DiaryEntry(
            date = LocalDate.of(2024, 1, 15),
            title = "Long Entry",
            body = longContent,
            tags = emptyList()
        )
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                EntryDisplayComponent(entry = testEntry)
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Show more").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed()
    }
    
    @Test
    fun emptyStateComponent_displaysCorrectMessage() {
        // Given
        val testDate = LocalDate.of(2024, 1, 15)
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                EmptyStateComponent(date = testDate)
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Create Entry").assertIsDisplayed()
        composeTestRule.onNodeWithText("You haven't written anything for this day yet.").assertIsDisplayed()
    }
    
    @Test
    fun emptyStateComponent_showsNotImplementedDialog() {
        // Given
        val testDate = LocalDate.of(2024, 1, 15)
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                EmptyStateComponent(date = testDate)
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Create Entry").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Feature Not Implemented").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entry creation and editing functionality will be implemented in a future version of the app.").assertIsDisplayed()
    }
    
    @Test
    fun dateDisplayComponent_showsCorrectDate() {
        // Given
        val testDate = LocalDate.of(2024, 1, 15)
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                DateDisplayComponent(
                    currentDate = testDate,
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Previous day").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next day").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to select date").assertIsDisplayed()
    }
    
    @Test
    fun dateDisplayComponent_navigationButtons_areClickable() {
        // Given
        val testDate = LocalDate.of(2024, 1, 15)
        var previousClicked = false
        var nextClicked = false
        
        composeTestRule.setContent {
            GeekDiaryTheme {
                DateDisplayComponent(
                    currentDate = testDate,
                    onPreviousDay = { previousClicked = true },
                    onNextDay = { nextClicked = true },
                    onDateSelected = {}
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Previous day").performClick()
        composeTestRule.onNodeWithContentDescription("Next day").performClick()
        
        // Then
        assert(previousClicked)
        assert(nextClicked)
    }
}
