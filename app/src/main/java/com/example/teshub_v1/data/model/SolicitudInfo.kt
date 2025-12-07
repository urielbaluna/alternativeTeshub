package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SolicitudInfo(
    @Json(name = "id_asesoria") val id: Int,
    @Json(name = "fecha_solicitud") val fecha: String,
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val imagen: String? = null,
    val carrera: String? = null
)