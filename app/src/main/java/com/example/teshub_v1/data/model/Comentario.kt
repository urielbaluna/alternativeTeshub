package com.example.teshub_v1.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Comentario(
    val matricula: String,
    val nombre: String,
    val imagen: String?,
    val comentario: String,
    val fecha: String?
)