package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class Publicacion(
    @Json(name = "id_publi") val id: Int,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    @Json(name = "calificacion_promedio") val calificacion: String?,
    @Json(name = "total_calificaciones") val totalVotos: Int,
    val integrantes: List<Integrante>?,
    val comentarios: List<Comentario>?
)

data class Integrante(
    val nombre: String
)
