package com.example.geekdiary.data.remote

import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error(NetworkException.ServerError)
            }
        } else {
            val errorMessage = response.errorBody()?.string() ?: "Unknown error"
            when (response.code()) {
                401 -> NetworkResult.Error(NetworkException.Unauthorized)
                in 400..499 -> NetworkResult.Error(
                    NetworkException.HttpError(response.code(), errorMessage)
                )
                in 500..599 -> NetworkResult.Error(NetworkException.ServerError)
                else -> NetworkResult.Error(
                    NetworkException.HttpError(response.code(), errorMessage)
                )
            }
        }
    } catch (e: UnknownHostException) {
        NetworkResult.Error(NetworkException.NetworkError)
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(NetworkException.Timeout)
    } catch (e: IOException) {
        NetworkResult.Error(NetworkException.NetworkError)
    } catch (e: Exception) {
        NetworkResult.Error(NetworkException.UnknownError(e))
    }
}
