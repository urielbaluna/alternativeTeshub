package com.example.teshub_v1.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Interes(
    @Json(name = "id_interes") val id: Int,
    val nombre: String
)