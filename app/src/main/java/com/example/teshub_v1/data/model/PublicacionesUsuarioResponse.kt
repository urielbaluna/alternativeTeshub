package com.example.teshub_v1.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicacionesUsuarioResponse(
    // El backend devuelve todo el objeto usuario, pero aquí solo nos importa la lista
    val publicaciones: List<PublicacionInfo>
)