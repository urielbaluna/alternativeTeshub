package com.example.teshub_v1.data.model

data class PublicacionInfo(
    val id_publi: Int,
    val proyecto_nombre: String,
    val hace_cuanto: String
)

data class PublicacionesUsuarioResponse(
    val publicaciones: List<PublicacionInfo>
)

