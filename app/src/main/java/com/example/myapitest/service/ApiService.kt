package com.example.minhaprimeiraapi.service

import com.example.myapitest.model.Car
import com.example.myapitest.model.RetrieveCar
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("car/")
    suspend fun getCars(): List<Car>

    @GET("car/{id}")
    suspend fun getCar(@Path("id") id: String): RetrieveCar

    @DELETE("car/{id}")
    suspend fun deleteCar(@Path("id") id: String)

    @POST("car")
    suspend fun addCar(@Body car: Car) : Car

    @PATCH("car/{id}")
    suspend fun updateCar(@Path("id") id: String, @Body car: Car): Car

}
