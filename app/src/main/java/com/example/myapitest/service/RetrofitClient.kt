package com.example.minhaprimeiraapi.service

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.0.119:3000/"

    val gson = GsonBuilder()
        .serializeNulls()
        .create()

    private val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService = instance.create(ApiService::class.java)
}