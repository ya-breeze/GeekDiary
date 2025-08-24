package com.example.geekdiary.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    suspend fun saveToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_TOKEN, token)
        }
    }
    
    suspend fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }
    
    suspend fun clearToken() {
        sharedPreferences.edit {
            remove(KEY_TOKEN)
        }
    }
    
    suspend fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
        }
    }
    
    suspend fun getCredentials(): Pair<String, String>? {
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val password = sharedPreferences.getString(KEY_PASSWORD, null)
        
        return if (email != null && password != null) {
            Pair(email, password)
        } else {
            null
        }
    }
    
    suspend fun clearCredentials() {
        sharedPreferences.edit {
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
        }
    }
    
    suspend fun clearAll() {
        sharedPreferences.edit {
            clear()
        }
    }
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
    }
}
