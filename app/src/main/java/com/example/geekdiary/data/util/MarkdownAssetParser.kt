package com.example.geekdiary.data.util

import java.util.regex.Pattern

/**
 * Utility class for parsing Markdown content to extract asset references
 * and transform URLs between backend and local formats.
 */
class MarkdownAssetParser {
    
    companion object {
        // Regex pattern to match ![](filename.ext) format
        private val ASSET_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)")
        
        // Common image and video file extensions
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv"
        )
        
        private const val BACKEND_ASSET_PATH = "/web/assets/"
    }
    
    /**
     * Extract asset filenames from Markdown content
     * @param markdownContent The markdown content to parse
     * @return List of asset filenames found in the content
     */
    fun extractAssetFilenames(markdownContent: String): List<String> {
        val assetFilenames = mutableListOf<String>()
        val matcher = ASSET_PATTERN.matcher(markdownContent)
        
        while (matcher.find()) {
            val url = matcher.group(2) // The URL part inside parentheses
            val filename = extractFilenameFromUrl(url)
            
            if (filename != null && isSupportedAssetFile(filename)) {
                assetFilenames.add(filename)
            }
        }
        
        return assetFilenames.distinct()
    }
    
    /**
     * Convert backend asset URLs to local file paths in Markdown content
     * @param markdownContent The markdown content with backend URLs
     * @param baseUrl The base URL of the backend server
     * @param assetPathMapping Map of filename to local file path
     * @return Markdown content with local file paths
     */
    fun convertBackendUrlsToLocal(
        markdownContent: String, 
        baseUrl: String,
        assetPathMapping: Map<String, String>
    ): String {
        var result = markdownContent
        val matcher = ASSET_PATTERN.matcher(markdownContent)
        
        while (matcher.find()) {
            val fullMatch = matcher.group(0) // Full ![alt](url) match
            val altText = matcher.group(1) // Alt text
            val url = matcher.group(2) // URL
            
            val filename = extractFilenameFromUrl(url)
            if (filename != null && assetPathMapping.containsKey(filename)) {
                val localPath = assetPathMapping[filename]
                val newMatch = "![$altText](file://$localPath)"
                result = result.replace(fullMatch, newMatch)
            }
        }
        
        return result
    }
    
    /**
     * Convert local file paths to backend URLs in Markdown content
     * @param markdownContent The markdown content with local file paths
     * @param baseUrl The base URL of the backend server
     * @return Markdown content with backend URLs
     */
    fun convertLocalUrlsToBackend(markdownContent: String, baseUrl: String): String {
        var result = markdownContent
        val matcher = ASSET_PATTERN.matcher(markdownContent)
        
        while (matcher.find()) {
            val fullMatch = matcher.group(0) // Full ![alt](url) match
            val altText = matcher.group(1) // Alt text
            val url = matcher.group(2) // URL
            
            if (url.startsWith("file://")) {
                // Extract filename from local file path
                val localPath = url.removePrefix("file://")
                val filename = localPath.substringAfterLast("/")
                
                if (isSupportedAssetFile(filename)) {
                    val backendUrl = "$baseUrl$BACKEND_ASSET_PATH$filename"
                    val newMatch = "![$altText]($backendUrl)"
                    result = result.replace(fullMatch, newMatch)
                }
            }
        }
        
        return result
    }
    
    /**
     * Convert relative asset filenames to full backend URLs
     * @param markdownContent The markdown content with relative filenames
     * @param baseUrl The base URL of the backend server
     * @return Markdown content with full backend URLs
     */
    fun convertRelativeToBackendUrls(markdownContent: String, baseUrl: String): String {
        var result = markdownContent
        val matcher = ASSET_PATTERN.matcher(markdownContent)
        
        while (matcher.find()) {
            val fullMatch = matcher.group(0) // Full ![alt](url) match
            val altText = matcher.group(1) // Alt text
            val url = matcher.group(2) // URL
            
            // Check if it's a relative filename (no protocol, no path separators except filename)
            if (!url.contains("://") && !url.startsWith("/") && isSupportedAssetFile(url)) {
                val backendUrl = "$baseUrl$BACKEND_ASSET_PATH$url"
                val newMatch = "![$altText]($backendUrl)"
                result = result.replace(fullMatch, newMatch)
            }
        }
        
        return result
    }
    
    /**
     * Extract filename from a URL or file path
     * @param url The URL or file path
     * @return The filename or null if not found
     */
    private fun extractFilenameFromUrl(url: String): String? {
        return try {
            // Handle different URL formats
            when {
                url.contains(BACKEND_ASSET_PATH) -> {
                    // Backend URL: extract filename after /web/assets/
                    url.substringAfter(BACKEND_ASSET_PATH).substringBefore("?")
                }
                url.startsWith("file://") -> {
                    // Local file path: extract filename
                    url.removePrefix("file://").substringAfterLast("/")
                }
                url.contains("/") -> {
                    // Other URL with path: extract last segment
                    url.substringAfterLast("/").substringBefore("?")
                }
                else -> {
                    // Assume it's just a filename
                    url.substringBefore("?")
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if a filename has a supported asset file extension
     * @param filename The filename to check
     * @return True if the file extension is supported
     */
    private fun isSupportedAssetFile(filename: String): Boolean {
        val extension = filename.substringAfterLast(".", "").lowercase()
        return extension.isNotEmpty() && SUPPORTED_EXTENSIONS.contains(extension)
    }
    
    /**
     * Check if Markdown content contains any asset references
     * @param markdownContent The markdown content to check
     * @return True if content contains asset references
     */
    fun hasAssetReferences(markdownContent: String): Boolean {
        return extractAssetFilenames(markdownContent).isNotEmpty()
    }
}
