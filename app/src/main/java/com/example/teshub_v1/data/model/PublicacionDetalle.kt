package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicacionDetalle(
    @Json(name = "id_publi") val id: Int,
    val nombre: String,
    val descripcion: String,
    val fecha: String?,
    val estado: String?, // "pendiente", "aprobado", etc.
    @Json(name = "imagen_portada") val imagenPortada: String?,
    @Json(name = "calificacion_promedio") val calificacion: String?,

    // Listas
    val archivos: List<String>? = emptyList(),
    val tags: List<String>? = emptyList(),
    val integrantes: List<IntegranteDetalle>? = emptyList(),
    val comentarios: List<Comentario>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class IntegranteDetalle(
    val matricula: String,
    val nombre: String,
    val apellido: String?,
    val imagen: String?
)