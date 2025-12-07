package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PerfilResponse(
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val imagen: String?,

    // --- NUEVOS CAMPOS ---
    val carrera: String?,
    val semestre: String?,
    val biografia: String?,
    val ubicacion: String?,
    val estado: Int?,

    val intereses: List<Interes>? = emptyList(),
    val estadisticas: EstadisticasRed? = null,

    @Json(name = "total_publicaciones") val totalPublicaciones: Int,
    @Json(name = "publicacion_destacada") val publicacionDestacada: String?
)
