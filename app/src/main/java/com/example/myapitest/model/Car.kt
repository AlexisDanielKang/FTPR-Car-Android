package com.example.myapitest.model

data class Car(
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: Location?
)

data class Location(
    val lat : String? = null,
    val long: String? = null
)

data class RetrieveCar(
    var id: String,
    var value: Car
)

