package com.example.geekdiary.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
        
        // Since minSdk is 34 (Android 14), we use modern media permissions
        val REQUIRED_STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    }
    
    /**
     * Check if storage permissions are granted
     * @return True if all required storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        // Since minSdk is 34, check modern media permissions
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
        // Since minSdk is 34, return missing modern media permissions
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
        // Since minSdk is 34, check rationale for modern media permissions
        return REQUIRED_STORAGE_PERMISSIONS.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }
}
