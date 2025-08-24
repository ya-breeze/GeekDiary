package com.example.geekdiary.data.util

import com.example.geekdiary.domain.repository.AssetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for transforming Markdown content between backend and local asset URLs
 */
@Singleton
class MarkdownLinkTransformer @Inject constructor(
    private val markdownAssetParser: MarkdownAssetParser,
    private val assetRepository: AssetRepository
) {
    
    /**
     * Transform Markdown content from backend format to local format for display
     * Converts backend asset URLs to local file paths where available
     * @param markdownContent The markdown content with backend URLs
     * @param baseUrl The base URL of the backend server
     * @return Markdown content with local file paths where available
     */
    suspend fun transformForDisplay(markdownContent: String, baseUrl: String): String {
        try {
            // Extract asset filenames from the content
            val assetFilenames = markdownAssetParser.extractAssetFilenames(markdownContent)
            
            // Build mapping of filename to local path for available assets
            val assetPathMapping = mutableMapOf<String, String>()
            
            for (filename in assetFilenames) {
                val localPath = assetRepository.getLocalFilePath(filename)
                if (localPath != null) {
                    assetPathMapping[filename] = localPath
                }
            }
            
            // Convert backend URLs to local paths where available
            return markdownAssetParser.convertBackendUrlsToLocal(
                markdownContent, 
                baseUrl, 
                assetPathMapping
            )
        } catch (e: Exception) {
            println("Error transforming markdown for display: ${e.message}")
            return markdownContent
        }
    }
    
    /**
     * Transform Markdown content from local format to backend format for sync
     * Converts local file paths to backend asset URLs
     * @param markdownContent The markdown content with local file paths
     * @param baseUrl The base URL of the backend server
     * @return Markdown content with backend URLs
     */
    suspend fun transformForSync(markdownContent: String, baseUrl: String): String {
        try {
            return markdownAssetParser.convertLocalUrlsToBackend(markdownContent, baseUrl)
        } catch (e: Exception) {
            println("Error transforming markdown for sync: ${e.message}")
            return markdownContent
        }
    }
    
    /**
     * Transform relative asset filenames to full backend URLs
     * This is used when processing content from the backend that contains relative references
     * @param markdownContent The markdown content with relative filenames
     * @param baseUrl The base URL of the backend server
     * @return Markdown content with full backend URLs
     */
    suspend fun transformRelativeToBackend(markdownContent: String, baseUrl: String): String {
        try {
            return markdownAssetParser.convertRelativeToBackendUrls(markdownContent, baseUrl)
        } catch (e: Exception) {
            println("Error transforming relative URLs to backend: ${e.message}")
            return markdownContent
        }
    }
    
    /**
     * Get display-ready content with stub placeholders for missing assets
     * @param markdownContent The markdown content
     * @param baseUrl The base URL of the backend server
     * @param stubImagePath Path to the stub/placeholder image
     * @return Markdown content with stubs for missing assets
     */
    suspend fun getDisplayContentWithStubs(
        markdownContent: String, 
        baseUrl: String,
        stubImagePath: String = "android_asset://stub_image.png"
    ): String {
        try {
            // First, transform available assets to local paths
            var transformedContent = transformForDisplay(markdownContent, baseUrl)
            
            // Extract remaining asset filenames (those that weren't transformed)
            val assetFilenames = markdownAssetParser.extractAssetFilenames(transformedContent)
            
            // Replace remaining backend URLs with stub placeholders
            for (filename in assetFilenames) {
                val isAvailable = assetRepository.isAssetAvailableLocally(filename)
                if (!isAvailable) {
                    // Replace with stub image
                    val backendUrl = "$baseUrl/web/assets/$filename"
                    transformedContent = transformedContent.replace(
                        "![]($backendUrl)",
                        "![]($stubImagePath)"
                    ).replace(
                        "![]($filename)",
                        "![]($stubImagePath)"
                    )
                }
            }
            
            return transformedContent
        } catch (e: Exception) {
            println("Error getting display content with stubs: ${e.message}")
            return markdownContent
        }
    }
    
    /**
     * Check if markdown content has any asset references that need transformation
     * @param markdownContent The markdown content to check
     * @return True if content has asset references
     */
    suspend fun hasAssetReferences(markdownContent: String): Boolean {
        return markdownAssetParser.hasAssetReferences(markdownContent)
    }
    
    /**
     * Get asset availability status for markdown content
     * @param markdownContent The markdown content
     * @return Map of filename to availability status
     */
    suspend fun getAssetAvailabilityStatus(markdownContent: String): Map<String, Boolean> {
        try {
            val assetFilenames = markdownAssetParser.extractAssetFilenames(markdownContent)
            val availabilityMap = mutableMapOf<String, Boolean>()
            
            for (filename in assetFilenames) {
                availabilityMap[filename] = assetRepository.isAssetAvailableLocally(filename)
            }
            
            return availabilityMap
        } catch (e: Exception) {
            println("Error getting asset availability status: ${e.message}")
            return emptyMap()
        }
    }
    
    /**
     * Transform content for editing (ensure all URLs are in a consistent format)
     * @param markdownContent The markdown content
     * @param baseUrl The base URL of the backend server
     * @param preferLocal Whether to prefer local paths over backend URLs
     * @return Transformed markdown content
     */
    suspend fun transformForEditing(
        markdownContent: String, 
        baseUrl: String,
        preferLocal: Boolean = true
    ): String {
        return if (preferLocal) {
            transformForDisplay(markdownContent, baseUrl)
        } else {
            transformRelativeToBackend(markdownContent, baseUrl)
        }
    }
    
    /**
     * Validate that all asset references in markdown content are accessible
     * @param markdownContent The markdown content
     * @return List of inaccessible asset filenames
     */
    suspend fun validateAssetReferences(markdownContent: String): List<String> {
        try {
            val assetFilenames = markdownAssetParser.extractAssetFilenames(markdownContent)
            val inaccessibleAssets = mutableListOf<String>()
            
            for (filename in assetFilenames) {
                if (!assetRepository.isAssetAvailableLocally(filename)) {
                    inaccessibleAssets.add(filename)
                }
            }
            
            return inaccessibleAssets
        } catch (e: Exception) {
            println("Error validating asset references: ${e.message}")
            return emptyList()
        }
    }
}
