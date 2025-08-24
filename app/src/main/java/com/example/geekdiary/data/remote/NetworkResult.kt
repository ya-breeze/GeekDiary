package com.example.geekdiary.data.remote

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val exception: NetworkException) : NetworkResult<T>()
    data class Loading<T>(val isLoading: Boolean = true) : NetworkResult<T>()
}

sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object NetworkError : NetworkException("Network connection error")
    object ServerError : NetworkException("Server error")
    data class HttpError(val code: Int, val errorMessage: String) : NetworkException("HTTP $code: $errorMessage")
    data class UnknownError(val originalException: Throwable) : NetworkException("Unknown error", originalException)
    object Unauthorized : NetworkException("Unauthorized - please login again")
    object Timeout : NetworkException("Request timeout")
}

inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (NetworkException) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(exception)
    return this
}

inline fun <T> NetworkResult<T>.onLoading(action: (Boolean) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Loading) action(isLoading)
    return this
}
