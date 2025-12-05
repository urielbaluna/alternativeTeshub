package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class CrearComentarioResponse(
    @Json(name = "mensaje")
    val mensaje: String
)
