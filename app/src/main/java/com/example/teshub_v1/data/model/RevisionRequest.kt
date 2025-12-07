package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RevisionRequest(
    @Json(name = "id_publi") val idPubli: Int,
    @Json(name = "nuevo_estado") val nuevoEstado: String, // "aprobado", "rechazado", "correcciones"
    val comentarios: String
)