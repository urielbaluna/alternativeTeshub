package com.example.teshub_v1.data.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AsesoradosResponse(
    val asesorados: List<PerfilResponse>
)