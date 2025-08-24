package com.example.geekdiary.data.local

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val ASSETS_DIRECTORY = "GeekDiary/Assets"
        private const val IMAGES_SUBDIRECTORY = "images"
        private const val VIDEOS_SUBDIRECTORY = "videos"
    }
    
    private val assetsDirectory: File by lazy {
        File(Environment.getExternalStorageDirectory(), ASSETS_DIRECTORY)
    }
    
    private val imagesDirectory: File by lazy {
        File(assetsDirectory, IMAGES_SUBDIRECTORY)
    }
    
    private val videosDirectory: File by lazy {
        File(assetsDirectory, VIDEOS_SUBDIRECTORY)
    }
    
    /**
     * Initialize storage directories
     * @return True if directories were created successfully
     */
    suspend fun initializeDirectories(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!assetsDirectory.exists()) {
                assetsDirectory.mkdirs()
            }
            if (!imagesDirectory.exists()) {
                imagesDirectory.mkdirs()
            }
            if (!videosDirectory.exists()) {
                videosDirectory.mkdirs()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save asset data to local storage
     * @param filename The filename of the asset
     * @param responseBody The response body containing asset data
     * @return The local file path if successful, null otherwise
     */
    suspend fun saveAsset(filename: String, responseBody: ResponseBody): String? = withContext(Dispatchers.IO) {
        try {
            val targetDirectory = getDirectoryForAsset(filename)
            val targetFile = File(targetDirectory, filename)
            
            // Ensure parent directory exists
            if (!targetDirectory.exists()) {
                targetDirectory.mkdirs()
            }
            
            // Write the file
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            targetFile.absolutePath
        } catch (e: IOException) {
            null
        }
    }
    
    /**
     * Get the local file path for an asset
     * @param filename The filename of the asset
     * @return The local file path
     */
    fun getLocalFilePath(filename: String): String {
        val targetDirectory = getDirectoryForAsset(filename)
        return File(targetDirectory, filename).absolutePath
    }
    
    /**
     * Check if an asset exists locally
     * @param filename The filename of the asset
     * @return True if the asset exists locally
     */
    suspend fun assetExists(filename: String): Boolean = withContext(Dispatchers.IO) {
        val targetDirectory = getDirectoryForAsset(filename)
        val targetFile = File(targetDirectory, filename)
        targetFile.exists() && targetFile.isFile
    }
    
    /**
     * Delete an asset from local storage
     * @param filename The filename of the asset to delete
     * @return True if the asset was deleted successfully
     */
    suspend fun deleteAsset(filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDirectory = getDirectoryForAsset(filename)
            val targetFile = File(targetDirectory, filename)
            if (targetFile.exists()) {
                targetFile.delete()
            } else {
                true // Consider it successful if file doesn't exist
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete an asset by its local file path
     * @param localFilePath The local file path of the asset
     * @return True if the asset was deleted successfully
     */
    suspend fun deleteAssetByPath(localFilePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(localFilePath)
            if (file.exists()) {
                file.delete()
            } else {
                true // Consider it successful if file doesn't exist
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the total size of all stored assets
     * @return Total size in bytes
     */
    suspend fun getTotalStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            calculateDirectorySize(assetsDirectory)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get the number of stored assets
     * @return Number of asset files
     */
    suspend fun getAssetCount(): Int = withContext(Dispatchers.IO) {
        try {
            countFilesInDirectory(assetsDirectory)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Check if external storage is available and writable
     * @return True if external storage is available
     */
    fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
    
    /**
     * Get the appropriate directory for an asset based on its file extension
     * @param filename The filename of the asset
     * @return The target directory
     */
    private fun getDirectoryForAsset(filename: String): File {
        val extension = filename.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> imagesDirectory
            "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv" -> videosDirectory
            else -> assetsDirectory // Default to main assets directory
        }
    }
    
    /**
     * Calculate the total size of a directory recursively
     * @param directory The directory to calculate size for
     * @return Total size in bytes
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
    
    /**
     * Count files in a directory recursively
     * @param directory The directory to count files in
     * @return Number of files
     */
    private fun countFilesInDirectory(directory: File): Int {
        var count = 0
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                count += if (file.isDirectory) {
                    countFilesInDirectory(file)
                } else {
                    1
                }
            }
        }
        return count
    }
}
