package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RevisionPendienteInfo(
    @Json(name = "id") val id: Int,
    val titulo: String,
    val descripcion: String?,
    val autor: String?,
    val fecha: String?,
    val estado: String?,
    @Json(name = "imagen") val imagenPortada: String? // Mapea el campo 'imagen' del backend
)