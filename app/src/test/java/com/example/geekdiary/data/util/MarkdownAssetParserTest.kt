package com.example.geekdiary.data.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarkdownAssetParserTest {
    
    private lateinit var parser: MarkdownAssetParser
    
    @Before
    fun setUp() {
        parser = MarkdownAssetParser()
    }
    
    @Test
    fun `extractAssetFilenames should extract image filenames from markdown`() {
        val markdown = """
            # My Diary Entry
            Here's an image: ![](photo1.jpg)
            And another: ![alt text](photo2.png)
            Some text here.
            ![](video.mp4)
        """.trimIndent()
        
        val result = parser.extractAssetFilenames(markdown)
        
        assertEquals(3, result.size)
        assertTrue(result.contains("photo1.jpg"))
        assertTrue(result.contains("photo2.png"))
        assertTrue(result.contains("video.mp4"))
    }
    
    @Test
    fun `extractAssetFilenames should handle empty content`() {
        val result = parser.extractAssetFilenames("")
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `extractAssetFilenames should ignore unsupported file types`() {
        val markdown = """
            ![](document.pdf)
            ![](image.jpg)
            ![](text.txt)
            ![](video.mp4)
        """.trimIndent()
        
        val result = parser.extractAssetFilenames(markdown)
        
        assertEquals(2, result.size)
        assertTrue(result.contains("image.jpg"))
        assertTrue(result.contains("video.mp4"))
        assertFalse(result.contains("document.pdf"))
        assertFalse(result.contains("text.txt"))
    }
    
    @Test
    fun `convertRelativeToBackendUrls should convert relative filenames to backend URLs`() {
        val markdown = "![](photo.jpg) and ![alt](video.mp4)"
        val baseUrl = "http://localhost:8080"
        
        val result = parser.convertRelativeToBackendUrls(markdown, baseUrl)
        
        assertTrue(result.contains("http://localhost:8080/web/assets/photo.jpg"))
        assertTrue(result.contains("http://localhost:8080/web/assets/video.mp4"))
    }
    
    @Test
    fun `convertBackendUrlsToLocal should convert backend URLs to local paths`() {
        val markdown = "![](http://localhost:8080/web/assets/photo.jpg)"
        val baseUrl = "http://localhost:8080"
        val assetPathMapping = mapOf("photo.jpg" to "/storage/assets/photo.jpg")
        
        val result = parser.convertBackendUrlsToLocal(markdown, baseUrl, assetPathMapping)
        
        assertTrue(result.contains("file:///storage/assets/photo.jpg"))
    }
    
    @Test
    fun `convertLocalUrlsToBackend should convert local file paths to backend URLs`() {
        val markdown = "![](file:///storage/assets/photo.jpg)"
        val baseUrl = "http://localhost:8080"
        
        val result = parser.convertLocalUrlsToBackend(markdown, baseUrl)
        
        assertTrue(result.contains("http://localhost:8080/web/assets/photo.jpg"))
    }
    
    @Test
    fun `hasAssetReferences should return true when assets are present`() {
        val markdown = "Some text ![](photo.jpg) more text"
        
        assertTrue(parser.hasAssetReferences(markdown))
    }
    
    @Test
    fun `hasAssetReferences should return false when no assets are present`() {
        val markdown = "Just some text without any images"
        
        assertFalse(parser.hasAssetReferences(markdown))
    }
    
    @Test
    fun `extractAssetFilenames should handle duplicate filenames`() {
        val markdown = """
            ![](photo.jpg)
            Some text
            ![](photo.jpg)
            ![](other.png)
        """.trimIndent()
        
        val result = parser.extractAssetFilenames(markdown)
        
        assertEquals(2, result.size)
        assertTrue(result.contains("photo.jpg"))
        assertTrue(result.contains("other.png"))
    }
    
    @Test
    fun `extractAssetFilenames should handle complex filenames`() {
        val markdown = """
            ![](uuid-123-456-789.jpg)
            ![](file_with_underscores.png)
            ![](file-with-dashes.mp4)
            ![](file.with.dots.jpeg)
        """.trimIndent()
        
        val result = parser.extractAssetFilenames(markdown)
        
        assertEquals(4, result.size)
        assertTrue(result.contains("uuid-123-456-789.jpg"))
        assertTrue(result.contains("file_with_underscores.png"))
        assertTrue(result.contains("file-with-dashes.mp4"))
        assertTrue(result.contains("file.with.dots.jpeg"))
    }
}
