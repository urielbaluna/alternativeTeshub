package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class RegistroEventoResponse(
    @Json(name = "mensaje")
    val mensaje: String,
    @Json(name = "asistentesRegistrados")
    val asistentesRegistrados: Int? = null,
    @Json(name = "cupoDisponible")
    val cupoDisponible: Int? = null
)
