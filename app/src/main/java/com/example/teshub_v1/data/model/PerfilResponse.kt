package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class PerfilResponse(
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val imagen: String?,

    @Json(name = "total_publicaciones") val totalPublicaciones: Int,
    @Json(name = "publicacion_destacada") val publicacionDestacada: String?
)

