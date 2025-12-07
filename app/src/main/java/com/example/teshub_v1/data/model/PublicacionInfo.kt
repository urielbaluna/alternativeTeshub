package com.example.teshub_v1.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicacionInfo(
    @Json(name = "id_publi") val id_publi: Int,
    @Json(name = "proyecto_nombre") val proyecto_nombre: String,
    @Json(name = "hace_cuanto") val hace_cuanto: String?,
    // Opcionales para evitar errores si el backend manda más datos
    val descripcion: String? = null,
    val imagen_portada: String? = null
)