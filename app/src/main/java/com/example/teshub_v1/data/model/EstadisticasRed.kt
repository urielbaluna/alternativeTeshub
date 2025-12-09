package com.example.teshub_v1.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EstadisticasRed(
    val seguidores: Int = 0,
    val seguidos: Int = 0,
    @Json(name = "total_publicaciones") val totalPublicaciones: Int = 0,
    val siguiendo: Boolean = false
)