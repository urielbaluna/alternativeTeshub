package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResponderSolicitudRequest(
    @Json(name = "id_asesoria") val idAsesoria: Int,
    val accion: String // "aceptar" o "rechazar"
)