package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicacionesUsuarioResponse(
    // Datos del Usuario
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val imagen: String?,
    val rol: String,
    val carrera: String?,

    // Estadísticas (para los contadores)
    val estadisticas: EstadisticasRed?,

    // Lista de Publicaciones
    val publicaciones: List<PublicacionInfo>?
)