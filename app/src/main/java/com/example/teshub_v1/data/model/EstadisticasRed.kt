package com.example.teshub_v1.data.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EstadisticasRed(
    val seguidores: Int = 0,
    val seguidos: Int = 0
)