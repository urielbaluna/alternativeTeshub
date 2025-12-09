package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class PerfilResponse(
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val imagen: String?,
    val carrera: String?,
    val semestre: String?,
    val biografia: String?,
    val ubicacion: String?,
    val estado: Int,
    val intereses: List<Interes>,
    val estadisticas: Estadisticas?,
    @Json(name = "total_publicaciones") val totalPublicaciones: Int,
    @Json(name = "publicacion_destacada") val publicacionDestacada: String?,
    var siguiendo: Boolean = false
)
data class Estadisticas(
    val seguidores: Int,
    val seguidos: Int
)