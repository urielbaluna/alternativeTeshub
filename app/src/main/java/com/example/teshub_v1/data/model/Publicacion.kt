package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Publicacion(
    // Identificador
    @Json(name = "id") val id: Int,

    // Datos principales (Coinciden con el JSON del backend: title, description, etc.)
    @Json(name = "title") val titulo: String,
    @Json(name = "description") val descripcion: String,
    @Json(name = "author") val autor: String?,
    @Json(name = "date") val fecha: String?,
    @Json(name = "image") val imagenPortada: String?,
    @Json(name = "type") val tipo: String?,

    @Json(name = "hace_cuanto") val haceCuanto: String?,

    // Métricas y Detalles
    val rating: String?,
    val downloads: Int = 0,
    val views: Int = 0,
    val tags: List<String>? = emptyList(),

    // Estos campos son opcionales porque a veces vienen en detalle, a veces no
    val integrantes: List<Integrante>? = null,
    val comentarios: List<Comentario>? = null
)

@JsonClass(generateAdapter = true)
data class Integrante(
    val matricula: String? = null, // Hacemos opcional por si acaso
    @Json(name = "nombre_completo") val nombre: String
)