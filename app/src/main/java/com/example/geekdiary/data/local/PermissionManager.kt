package com.example.geekdiary.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        const val STORAGE_PERMISSION_REQUEST_CODE = 1001
        
        val REQUIRED_STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need WRITE_EXTERNAL_STORAGE for app-specific directories
            emptyArray<String>()
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * Check if storage permissions are granted
     * @return True if all required storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        // For Android 13+, we don't need storage permissions for app-specific directories
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        
        return REQUIRED_STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if a specific permission is granted
     * @param permission The permission to check
     * @return True if the permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get the list of required storage permissions that are not yet granted
     * @return Array of permissions that need to be requested
     */
    fun getRequiredStoragePermissions(): Array<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return emptyArray()
        }
        
        return REQUIRED_STORAGE_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * Check if we should show rationale for storage permissions
     * @param activity The activity context
     * @return True if rationale should be shown
     */
    fun shouldShowStoragePermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        
        return REQUIRED_STORAGE_PERMISSIONS.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }
}
