package com.example.minhaprimeiraapi.service

import android.util.Log
import retrofit2.HttpException

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val code: Int, val message: String) : Result<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (e: Exception) {

        Log.e("ApiCall", "Error: ${e.localizedMessage}", e)

        when (e) {
            is HttpException -> {
                Log.e("safeApiCall", "HTTP error: code=${e.code()}, message=${e.message()}")
                Result.Error(e.code(), e.message())
            }
            else -> {
                Log.e("safeApiCall", "Unknown error: ${e.localizedMessage}")
                Result.Error(-1, e.toString())
            }
        }
    }
}