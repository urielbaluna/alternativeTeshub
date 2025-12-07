package com.example.teshub_v1.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MiAsesorResponse(
    val mensaje: String?,
    val asesor: PerfilResponse?,
    @Json(name = "estado_texto") val estado: String?
)