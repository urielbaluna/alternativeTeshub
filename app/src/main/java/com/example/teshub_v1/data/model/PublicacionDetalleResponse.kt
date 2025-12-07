package com.example.teshub_v1.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicacionDetalleResponse(
    val publicacion: PublicacionDetalle
)