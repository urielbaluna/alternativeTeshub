package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class CrearEventoResponse(
    @Json(name = "mensaje")
    val mensaje: String
)
