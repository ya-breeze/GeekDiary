package com.example.geekdiary.data.remote.interceptor

import com.example.geekdiary.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip authentication for the authorize endpoint
        if (originalRequest.url.encodedPath.contains("/v1/authorize")) {
            return chain.proceed(originalRequest)
        }
        
        // Get token and add to request
        val token = runBlocking { tokenManager.getToken() }
        
        val authenticatedRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(authenticatedRequest)
    }
}
