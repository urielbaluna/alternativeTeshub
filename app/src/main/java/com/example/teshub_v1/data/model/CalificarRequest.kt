package com.example.teshub_v1.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CalificarRequest(
    val id_publi: Int,
    val evaluacion: Int
)