package com.example.geekdiary.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    
    suspend fun getServerUrl(): String {
        return sharedPreferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }
    
    suspend fun setServerUrl(url: String) {
        sharedPreferences.edit {
            putString(KEY_SERVER_URL, url)
        }
    }
    
    suspend fun resetToDefaults() {
        sharedPreferences.edit {
            remove(KEY_SERVER_URL)
        }
    }
    
    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://localhost:8080"
    }
}
