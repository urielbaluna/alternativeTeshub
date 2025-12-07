package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistorialResponse(
    val historial: List<RevisionItem>
)

@JsonClass(generateAdapter = true)
data class RevisionItem(
    @Json(name = "estado_asignado") val estado: String,
    val comentarios: String,
    @Json(name = "fecha_revision") val fecha: String,
    @Json(name = "asesor_nombre") val nombreAsesor: String
)