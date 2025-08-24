package com.example.geekdiary.data.mapper

import com.example.geekdiary.data.remote.dto.ItemsResponseDto
import com.example.geekdiary.domain.model.DiaryEntry
import org.junit.Test
import java.time.LocalDate

class DiaryEntryMapperTest {
    
    @Test
    fun `ItemsResponseDto toDomain should map correctly`() {
        // Given
        val dto = ItemsResponseDto(
            date = "2024-01-15",
            title = "Test Entry",
            body = "This is a test entry",
            tags = listOf("test", "example"),
            previousDate = "2024-01-14",
            nextDate = "2024-01-16"
        )
        
        // When
        val domain = dto.toDomain()
        
        // Then
        assert(domain.date == LocalDate.of(2024, 1, 15))
        assert(domain.title == "Test Entry")
        assert(domain.body == "This is a test entry")
        assert(domain.tags == listOf("test", "example"))
        assert(domain.previousDate == LocalDate.of(2024, 1, 14))
        assert(domain.nextDate == LocalDate.of(2024, 1, 16))
        assert(!domain.isLocal)
        assert(!domain.needsSync)
    }
    
    @Test
    fun `DiaryEntry toRequestDto should map correctly`() {
        // Given
        val entry = DiaryEntry(
            date = LocalDate.of(2024, 1, 15),
            title = "Test Entry",
            body = "This is a test entry",
            tags = listOf("test", "example")
        )
        
        // When
        val dto = entry.toRequestDto()
        
        // Then
        assert(dto.date == "2024-01-15")
        assert(dto.title == "Test Entry")
        assert(dto.body == "This is a test entry")
        assert(dto.tags == listOf("test", "example"))
    }
    
    @Test
    fun `ItemsResponseDto with null dates should map correctly`() {
        // Given
        val dto = ItemsResponseDto(
            date = "2024-01-15",
            title = "Test Entry",
            body = "This is a test entry",
            tags = emptyList(),
            previousDate = null,
            nextDate = null
        )
        
        // When
        val domain = dto.toDomain()
        
        // Then
        assert(domain.date == LocalDate.of(2024, 1, 15))
        assert(domain.previousDate == null)
        assert(domain.nextDate == null)
        assert(domain.tags.isEmpty())
    }
    
    @Test
    fun `DiaryEntry with null dates should map correctly`() {
        // Given
        val entry = DiaryEntry(
            date = LocalDate.of(2024, 1, 15),
            title = "Test Entry",
            body = "This is a test entry",
            tags = emptyList(),
            previousDate = null,
            nextDate = null
        )
        
        // When
        val dto = entry.toResponseDto()
        
        // Then
        assert(dto.date == "2024-01-15")
        assert(dto.previousDate == null)
        assert(dto.nextDate == null)
        assert(dto.tags.isEmpty())
    }
}
