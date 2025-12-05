package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class Comentario(
    @Json(name = "comentario")
    val comentario: String,

    @Json(name = "nombre")
    val nombre: String,

    @Json(name = "matricula")
    val matricula: String?
)
