package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class PublicacionDetalleResponse(
    @Json(name = "publicacion")
    val publicacion: Publicacion
)
